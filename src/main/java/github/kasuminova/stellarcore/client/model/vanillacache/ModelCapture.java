package github.kasuminova.stellarcore.client.model.vanillacache;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;

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

    /**
     * Scopes are closed by a RETURN injection, which an exception escaping the
     * model loader skips — and the loader fails plenty of models on purpose. A
     * leaked scope is therefore expected, but a stack this deep means the pops
     * are not keeping up, so it is dropped rather than grown. Dropping it costs
     * nothing but the fallback read of the models still on it.
     */
    private static final int MAX_DEPTH = 16;

    private static final ThreadLocal<Deque<ModelCapture>> SCOPES =
            ThreadLocal.withInitial(ArrayDeque::new);

    private final ByteArrayOutputStream modelJson = new ByteArrayOutputStream(8192);
    private final ByteArrayOutputStream armatureJson = new ByteArrayOutputStream(1024);

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
        final Deque<ModelCapture> scopes = SCOPES.get();
        if (scopes.size() >= MAX_DEPTH) {
            scopes.clear();
        }
        scopes.push(new ModelCapture());
    }

    /** Close the innermost scope. Tolerates a stack left empty by a leaked pop. */
    public static void end() {
        final Deque<ModelCapture> scopes = SCOPES.get();
        if (!scopes.isEmpty()) {
            scopes.pop();
        }
    }

    public static boolean isActive() {
        return !SCOPES.get().isEmpty();
    }

    /** Tap a model JSON stream, or return it untouched outside a scope. */
    public static InputStream teeModelJson(@Nullable final InputStream delegate) {
        return tap(delegate, sink(false));
    }

    /** Tap an armature stream, or return it untouched outside a scope. */
    public static InputStream teeArmature(@Nullable final InputStream delegate) {
        return tap(delegate, sink(true));
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
        return SCOPES.get().peek();
    }

    private static ByteArrayOutputStream sink(final boolean armature) {
        final ModelCapture scope = current();
        if (scope == null) {
            return null;
        }
        return armature ? scope.armatureJson : scope.modelJson;
    }

    @Nullable
    private static byte[] drain(final boolean armature) {
        final ModelCapture scope = current();
        if (scope == null) {
            return null;
        }
        final ByteArrayOutputStream buffer = armature ? scope.armatureJson : scope.modelJson;
        return buffer.size() == 0 ? null : buffer.toByteArray();
    }

    @Nullable
    private static InputStream tap(@Nullable final InputStream delegate, @Nullable final ByteArrayOutputStream sink) {
        if (delegate == null || sink == null) {
            return delegate;
        }
        return new FilterInputStream(delegate) {
            @Override
            public int read() throws IOException {
                final int value = super.read();
                if (value >= 0) {
                    sink.write(value);
                }
                return value;
            }

            @Override
            public int read(final byte[] buffer, final int offset, final int length) throws IOException {
                final int read = super.read(buffer, offset, length);
                if (read > 0) {
                    sink.write(buffer, offset, read);
                }
                return read;
            }
        };
    }
}
