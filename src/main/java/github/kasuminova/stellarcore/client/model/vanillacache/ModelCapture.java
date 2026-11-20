package github.kasuminova.stellarcore.client.model.vanillacache;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The bytes the model loader is reading right now, for the model it is loading
 * right now.
 *
 * <h2>Why the loader's own reads are tapped</h2>
 * A snapshot has to hold exactly what the loader parsed. Asking the resource
 * manager for the same file a second time is not equivalent: it costs one extra
 * lookup per model on the critical path, and on a machine where that lookup
 * happens to come back empty the model cache stays empty forever — silently,
 * because the read failure had nowhere to be reported — while the blockstate
 * cache, which tees the loader's own read, keeps working on the very same
 * machine. Tapping both reads removes the second lookup instead of hoping it
 * succeeds.
 *
 * <h2>Scope</h2>
 * A scope spans one {@code ModelLoader$VanillaLoader.loadModel} call, which
 * reads the model's armature first and its model JSON second, so both files land
 * in the same scope: the armature through {@link TeeingResourceManager}, the
 * model JSON through the resource tap in {@code ModelBakery.loadModel}.
 *
 * <p>Scopes nest, because loading a model can load its parents, so each thread
 * owns a stack of them. Every scope is opened and closed by the same thread
 * inside a single method call, and a model's reads and its capture happen in
 * that same frame with no nested scope opened in between, so the capture always
 * reads the scope its own reads wrote to.</p>
 */
public final class ModelCapture {

    private static final ThreadLocal<ObjectArrayList<ModelCapture>> SCOPES =
        ThreadLocal.withInitial(ObjectArrayList::new);

    @Nullable
    private CaptureBuffer modelJson;
    @Nullable
    private CaptureBuffer armatureJson;

    /** Whether the loader's own armature lookup reported the file missing. */
    private boolean armatureAbsent = false;

    private ModelCapture() {
    }

    /**
     * Open a scope for the model about to be loaded.
     *
     * <p>Only useful while the disk cache is enabled; with it off nothing opens
     * a scope and every tap below degrades to a pass-through.</p>
     */
    public static void begin() {
        SCOPES.get().add(new ModelCapture());
    }

    /** Close the innermost scope. Tolerates a stack left empty by a leaked pop. */
    public static void end() {
        final ObjectArrayList<ModelCapture> scopes = SCOPES.get();
        if (!scopes.isEmpty()) {
            scopes.remove(scopes.size() - 1);
        }
    }

    public static boolean isActive() {
        return !SCOPES.get().isEmpty();
    }

    /** Tap a model JSON stream, or return it untouched outside a scope. */
    public static InputStream teeModelJson(@Nullable final InputStream delegate) {
        return tap(delegate, false);
    }

    /** Tap an armature stream, or return it untouched outside a scope. */
    public static InputStream teeArmature(@Nullable final InputStream delegate) {
        return tap(delegate, true);
    }

    /**
     * The model JSON the loader read, or {@code null} outside a scope or when the
     * load never got as far as the JSON — the caller falls back to reading it
     * itself.
     */
    @Nullable
    public static byte[] modelJson() {
        return drain(false);
    }

    /**
     * The armature the loader read, or {@code null} when there is none, it could
     * not be read, or the load never reached the armature.
     */
    @Nullable
    public static byte[] armatureJson() {
        return drain(true);
    }

    /**
     * Cache snapshots treat these arrays as immutable; transfer a full buffer without copying.
     */
    @Nullable
    static byte[] modelJsonForCache() {
        return drainForCache(false);
    }

    @Nullable
    static byte[] armatureJsonForCache() {
        return drainForCache(true);
    }

    /**
     * Whether the loader's own armature lookup failed with "file not found",
     * which is Forge's own verdict that this model has no armature. Only a tap
     * that saw that exact failure may be trusted as a verdict; an armature that
     * was simply never read is unknown, not absent.
     */
    public static boolean armatureAbsent() {
        final ModelCapture scope = current();
        return scope != null && scope.armatureAbsent;
    }

    static void markArmatureAbsent() {
        final ModelCapture scope = current();
        if (scope != null) {
            scope.armatureAbsent = true;
        }
    }

    // ------------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------------

    @Nullable
    private static ModelCapture current() {
        final ObjectArrayList<ModelCapture> scopes = SCOPES.get();
        return scopes.isEmpty() ? null : scopes.get(scopes.size() - 1);
    }

    @Nullable
    private static byte[] drain(final boolean armature) {
        final ModelCapture scope = current();
        if (scope == null) {
            return null;
        }
        final ByteArrayOutputStream buffer = armature ? scope.armatureJson : scope.modelJson;
        return buffer == null || buffer.size() == 0 ? null : buffer.toByteArray();
    }

    @Nullable
    private static byte[] drainForCache(final boolean armature) {
        final ModelCapture scope = current();
        if (scope == null) {
            return null;
        }
        final CaptureBuffer buffer = armature ? scope.armatureJson : scope.modelJson;
        return buffer == null || buffer.size() == 0 ? null : buffer.bytesForCache();
    }

    @Nullable
    private static InputStream tap(@Nullable final InputStream delegate, final boolean armature) {
        // Missing armatures and cache hits never need a capture array.
        final ModelCapture scope = current();
        if (delegate == null || scope == null) {
            return delegate;
        }
        return new FilterInputStream(delegate) {
            @Nullable
            private CaptureBuffer buffer;

            private CaptureBuffer buffer(final int firstReadLength) {
                if (buffer == null) {
                    buffer = scope.sink(armature, firstReadLength);
                }
                return buffer;
            }

            @Override
            public int read() throws IOException {
                final int value = in.read();
                if (value >= 0) {
                    buffer(1).write(value);
                }
                return value;
            }

            @Override
            public int read(@Nonnull final byte[] buffer, final int offset, final int length) throws IOException {
                final int read = in.read(buffer, offset, length);
                if (read > 0) {
                    buffer(read).write(buffer, offset, read);
                }
                return read;
            }
        };
    }

    private CaptureBuffer sink(final boolean armature, final int firstReadLength) {
        if (armature) {
            if (armatureJson == null) {
                armatureJson = new CaptureBuffer(firstReadLength);
            }
            return armatureJson;
        }
        if (modelJson == null) {
            modelJson = new CaptureBuffer(firstReadLength);
        }
        return modelJson;
    }

    private static final class CaptureBuffer extends ByteArrayOutputStream {
        private CaptureBuffer(final int firstReadLength) {
            super(Math.max(256, firstReadLength));
        }

        private byte[] bytesForCache() {
            // A later append to a full buffer must grow it before writing, so
            // previously published arrays remain unchanged. Partially full
            // buffers still need a compact copy; reset is never exposed.
            return count == buf.length ? buf : toByteArray();
        }
    }

}
