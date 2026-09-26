package github.kasuminova.stellarcore.client.model.vanillacache;

import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.minecraft.forge.vanillamodeldiskcache.AccessorMinecraftResourcePacks;
import github.kasuminova.stellarcore.mixin.util.StellarCoreAbstractResourcePackAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraftforge.common.ForgeVersion;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Environment fingerprint for the vanilla model disk cache.
 *
 * <h2>What is trusted</h2>
 * The contract is: if the mod list is unchanged and the model-related content of
 * the ordered resource pack list is unchanged, the model JSONs those resources
 * expose are unchanged too. A cache hit therefore never inspects model content;
 * the fingerprint alone decides whether a cache file may be reused.
 *
 * <h2>What the fingerprint covers</h2>
 * <ul>
 *   <li><b>Resource packs</b> in priority order, which is where every model
 *       comes from. FML registers each mod as one of these, so a mod's models are
 *       covered here and do not need a separate mod-list component. A zip pack
 *       contributes the CRC of each relevant entry, read from the central
 *       directory at no decompression cost; a directory pack contributes the
 *       path, size and mtime of each relevant file.</li>
 *   <li><b>Mod ids and versions are deliberately not part of this.</b> They
 *       describe code, not model resources, and a version string changes on every
 *       development build even when the models are untouched.</li>
 *   <li>Only {@code models/}, {@code blockstates/} and {@code armatures/} are
 *       considered. Editing a texture or a language file does not invalidate the
 *       cache, and neither does recompiling a mod.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * {@link #prewarmAsync()} is expected to be called during FML's construction
 * phase, which leaves the whole preInit + init window for the scan to finish
 * before the model loader starts. It returns a future so the caller can chain the
 * cache pre-read directly off it: the disk is then read on the same background
 * thread, while the game is still loading mods rather than waiting for models.
 */
public final class EnvironmentFingerprint {

    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    /** Directory subtrees whose contents influence model/animation loading. */
    private static final String[] TRACKED_SUBTREES = {"models", "blockstates", "armatures"};

    private static volatile long seed = 0L;
    private static volatile boolean ready = false;

    /**
     * The running (or finished) fingerprint scan. Completed with {@code null} when
     * the scan failed, so waiters can distinguish "no answer" from "answer is 0".
     * Callers chain the cache pre-read off this future so the disk is only touched
     * once the environment is known.
     */
    private static volatile CompletableFuture<Long> fingerprintFuture = null;

    private EnvironmentFingerprint() {
    }

    /**
     * Start (or join) the asynchronous fingerprint scan.
     *
     * @return a future completed with the seed, or with {@code null} if the scan
     *         failed. The caller may chain further background work off it; the
     *         chained stage runs on the scan thread, so the whole prepare
     *         pipeline needs only one background thread.
     */
    public static CompletableFuture<Long> prewarmAsync() {
        final CompletableFuture<Long> existing = fingerprintFuture;
        if (existing != null) {
            return existing;
        }
        synchronized (EnvironmentFingerprint.class) {
            if (fingerprintFuture != null) {
                return fingerprintFuture;
            }
            final CompletableFuture<Long> future = new CompletableFuture<>();
            fingerprintFuture = future;
            final Thread scanner = new Thread(() -> {
                try {
                    final long computed = compute();
                    seed = computed;
                    ready = true;
                    future.complete(computed);
                } catch (Throwable t) {
                    StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Fingerprint scan failed", t);
                    // A null result means "unknown", which makes the cache unusable
                    // for this session. That is the safe direction: we would rather
                    // load everything the slow way than reuse a wrong cache.
                    future.complete(null);
                }
            }, "StellarCore-EnvironmentFingerprint");
            scanner.setDaemon(true);
            scanner.start();
            return future;
        }
    }

    /**
     * Recompute the fingerprint synchronously on the calling thread. Used after a
     * resource reload, where the environment may have changed and the answer is
     * needed before deciding whether the cache can be inherited.
     */
    public static synchronized long recomputeNow() {
        Long computed = null;
        try {
            computed = compute();
        } catch (Throwable t) {
            StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Fingerprint recompute failed", t);
        }
        if (computed == null) {
            ready = false;
            fingerprintFuture = null;
            return 0L;
        }
        seed = computed;
        ready = true;
        fingerprintFuture = CompletableFuture.completedFuture(computed);
        return computed;
    }

    public static boolean isReady() {
        return ready;
    }

    /** Non-blocking read; only meaningful when {@link #isReady()} is true. */
    public static long seedOrZero() {
        return ready ? seed : 0L;
    }

    // ------------------------------------------------------------------------
    // Scan
    // ------------------------------------------------------------------------

    private static long compute() {
        long hash = FNV_OFFSET_BASIS;
        hash = mixForgeVersion(hash);
        hash = mixResourcePacks(hash);
        return hash;
    }

    private static long mixForgeVersion(long hash) {
        hash = mix(hash, ForgeVersion.mcVersion);
        hash = mix(hash, ForgeVersion.getVersion());
        return hash;
    }

    private static long mixResourcePacks(long hash) {
        final Minecraft minecraft;
        try {
            minecraft = Minecraft.getMinecraft();
        } catch (Throwable t) {
            return mix(hash, "no-minecraft");
        }
        if (minecraft == null) {
            return mix(hash, "no-minecraft");
        }

        final AccessorMinecraftResourcePacks accessor = (AccessorMinecraftResourcePacks) minecraft;

        // Order matters: a pack earlier in the list overrides one later.
        int index = 0;
        try {
            for (final IResourcePack pack : accessor.stellar_core$getDefaultResourcePacks()) {
                hash = mixPack(hash, index++, pack);
            }
        } catch (Throwable t) {
            hash = mix(hash, "default-packs-failed");
        }

        try {
            final ResourcePackRepository repository = accessor.stellar_core$getResourcePackRepository();
            if (repository != null) {
                for (final ResourcePackRepository.Entry entry : repository.getRepositoryEntries()) {
                    hash = mixPack(hash, index++, entry.getResourcePack());
                }
            }
        } catch (Throwable t) {
            StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Failed to fingerprint resource packs", t);
            hash = mix(hash, "packs-failed");
        }
        return hash;
    }

    private static long mixPack(long hash, final int index, @Nullable final IResourcePack pack) {
        hash = mixLong(hash, index);
        if (pack == null) {
            return mix(hash, "null-pack");
        }
        hash = mix(hash, pack.getPackName());
        if (pack instanceof StellarCoreAbstractResourcePackAccessor accessor) {
            final File root = accessor.stellar_core$getResourcePackFile();
            if (root == null) {
                return hash;
            }
            // A zip pack is fingerprinted by the CRCs of the entries we care
            // about; a directory pack by the identity of the files under the same
            // subtrees. Both cover only model-related content, so editing a
            // texture or a lang file leaves the cache valid.
            hash = root.isFile()
                    ? mixArchive(hash, root)
                    : mixDirectory(hash, root);
        }
        return hash;
    }

    private static long mixArchive(long hash, final File archive) {
        try (ZipFile zip = new ZipFile(archive)) {
            final List<ZipEntry> relevant = new ArrayList<>();
            final Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && isTrackedResource(entry.getName())) {
                    relevant.add(entry);
                }
            }
            relevant.sort(Comparator.comparing(ZipEntry::getName));
            for (final ZipEntry entry : relevant) {
                hash = mix(hash, entry.getName());
                hash = mixLong(hash, entry.getCrc());
                hash = mixLong(hash, entry.getSize());
            }
        } catch (Throwable t) {
            StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Failed to fingerprint archive {}",
                    archive.getAbsolutePath(), t);
            hash = mix(hash, "archive-failed:" + archive.getName());
        }
        return hash;
    }

    /**
     * Fingerprint a directory pack from the files under the tracked subtrees.
     *
     * <p>Only {@code models/}, {@code blockstates/} and {@code armatures/} are
     * walked. A compiler writing class files never touches those paths, so the
     * churn that affects a mod's build output does not reach here.</p>
     */
    private static long mixDirectory(long hash, final File root) {
        if (!root.isDirectory()) {
            return mix(hash, "not-a-directory");
        }
        for (final TrackedFile file : collectTrackedFiles(root)) {
            hash = mix(hash, file.relativePath);
            hash = mixLong(hash, file.size);
            hash = mixLong(hash, file.lastModified);
        }
        return hash;
    }

    private static List<TrackedFile> collectTrackedFiles(final File root) {
        final Path rootPath = root.toPath();
        final List<TrackedFile> collected = new ArrayList<>();
        final Path assets = rootPath.resolve("assets");
        if (!Files.isDirectory(assets)) {
            return collected;
        }

        try (DirectoryStream<Path> namespaces =
                     Files.newDirectoryStream(assets)) {
            for (final Path namespace : namespaces) {
                if (!Files.isDirectory(namespace)) {
                    continue;
                }
                for (final String subtree : TRACKED_SUBTREES) {
                    final Path dir = namespace.resolve(subtree);
                    if (Files.isDirectory(dir)) {
                        walkedBy(dir, rootPath, collected);
                    }
                }
            }
        } catch (Throwable t) {
            StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Failed to walk {}", root, t);
        }

        collected.sort(Comparator.comparing(file -> file.relativePath));
        return collected;
    }

    /** Recursively collect files under {@code dir} with one attribute read each. */
    private static void walkedBy(final Path dir,
                                 final Path root,
                                 final List<TrackedFile> sink) {
        try (DirectoryStream<Path> children =
                     Files.newDirectoryStream(dir)) {
            for (final Path child : children) {
                final BasicFileAttributes attributes;
                try {
                    attributes = Files.readAttributes(
                            child, BasicFileAttributes.class);
                } catch (Throwable ignored) {
                    // A file that vanished mid-walk simply does not contribute.
                    continue;
                }
                if (attributes.isDirectory()) {
                    walkedBy(child, root, sink);
                    continue;
                }
                // Forward slashes so the value does not depend on the host.
                final String relative = root.relativize(child).toString().replace('\\', '/');
                sink.add(new TrackedFile(relative, attributes.size(), attributes.lastModifiedTime().toMillis()));
            }
        } catch (Throwable ignored) {
            // An unreadable subtree contributes nothing rather than failing the scan.
        }
    }

    /** One tracked resource file: enough identity to detect a content change. */
    private static final class TrackedFile {
        final String relativePath;
        final long size;
        final long lastModified;

        TrackedFile(final String relativePath, final long size, final long lastModified) {
            this.relativePath = relativePath;
            this.size = size;
            this.lastModified = lastModified;
        }
    }

    /**
     * Whether a resource path falls in a subtree the model cache depends on.
     * Paths have the form {@code assets/<namespace>/<kind>/...}.
     */
    private static boolean isTrackedResource(final String path) {
        if (!path.startsWith("assets/")) {
            return false;
        }
        final int namespaceEnd = path.indexOf('/', "assets/".length());
        if (namespaceEnd < 0) {
            return false;
        }
        final String rest = path.substring(namespaceEnd + 1);
        for (final String subtree : TRACKED_SUBTREES) {
            if (rest.startsWith(subtree + "/")) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // FNV-1a helpers
    // ------------------------------------------------------------------------

    private static long mix(long hash, @Nullable final String s) {
        if (s == null) {
            return hash ^ 0x9E3779B97F4A7C15L;
        }
        for (int i = 0; i < s.length(); i++) {
            hash ^= s.charAt(i);
            hash *= FNV_PRIME;
        }
        return hash;
    }

    private static long mixLong(long hash, final long value) {
        hash ^= value;
        hash *= FNV_PRIME;
        return hash;
    }

}
