package github.kasuminova.stellarcore.client.model.vanillacache;

import com.google.common.collect.ImmutableMap;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.animation.ModelBlockAnimation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Cross-restart disk cache for vanilla-format JSON models.
 *
 * <h2>Validity contract</h2>
 * Correctness rests entirely on {@link EnvironmentFingerprint}. The fingerprint
 * covers the mod list (id, version, jar file identity) and the ordered list of
 * resource packs (name, physical file identity, plus the aggregate mtime of each
 * pack's {@code models/} and {@code armatures/} subtrees). It is baked into the
 * cache file <em>name</em>, so a changed environment simply computes a different
 * file name and this cache is never consulted.
 *
 * <p>Because of that, a cache hit performs <strong>no IO at all</strong>: the
 * bytes were read once at startup, and a hit is a map lookup plus a Gson parse.
 * The previous design re-read and re-hashed every model on every hit, which cost
 * as much as not caching at all.</p>
 *
 * <h2>Self-healing</h2>
 * If a cached entry fails to parse (truncated write, hand-edited file, a mod that
 * produces JSON Gson rejects), the entry is dropped from the in-memory view and
 * the caller falls through to the normal loader, which re-captures it. A bad
 * entry therefore repairs itself on the next load rather than persisting.</p>
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class VanillaModelDiskCache {

    public static final VanillaModelDiskCache INSTANCE = new VanillaModelDiskCache();

    private static final String FILE_PREFIX = "stellarcore_vanilla_model_cache_";
    private static final String BLOCKSTATE_PREFIX = "stellarcore_blockstate_cache_";
    private static final String FILE_SUFFIX = ".dat";

    /** Every cache file we own, plus this run's temp files, starts with one of these. */
    private static final String[] CACHE_PREFIXES = {FILE_PREFIX, BLOCKSTATE_PREFIX};

    /**
     * Temp files are only ever visible mid-write, so one this old means a run
     * died between creating and moving it. The threshold stays small but non-zero
     * to avoid deleting a temp file that a concurrent write is still feeding.
     */
    private static final long STALE_TMP_AGE_MILLIS = 60L * 1000;

    /**
     * The animation object Forge substitutes when a model has no armature file.
     * Behaviourally identical to Forge's own default, but we construct it
     * directly so a hit never has to call into the resource manager.
     */
    private static final ModelBlockAnimation EMPTY_ANIMATION =
            new ModelBlockAnimation(ImmutableMap.of(), ImmutableMap.of());

    /** Snapshots captured during this run, to be persisted by {@link #saveAsync}. */
    private final Map<ResourceLocation, VanillaModelSnapshot> live = new ConcurrentHashMap<>();

    /** Blockstates captured during this run, persisted alongside the models. */
    private final Map<ResourceLocation, BlockstateSnapshot> liveBlockstates = new ConcurrentHashMap<>();

    /** Snapshots read from disk for the current environment; the hit source. */
    private volatile Map<ResourceLocation, VanillaModelSnapshot> diskView = null;

    /** Blockstates read from disk for the current environment; the hit source. */
    private volatile Map<ResourceLocation, BlockstateSnapshot> blockstateView = null;

    private volatile long loadedSeed = 0L;

    /** The fingerprint-then-read pipeline started by {@link #prepareAsync}. */
    private volatile CompletableFuture<Void> prepareFuture = null;
    private volatile boolean prepared = false;

    private VanillaModelDiskCache() {
    }

    public boolean isEnabled() {
        return StellarCoreConfig.PERFORMANCE.forge.vanillaModelDiskCache;
    }

    // ------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------

    /**
     * Kick off the whole prepare pipeline on a background thread: first wait for
     * the environment fingerprint, then read and decode the cache file for that
     * environment.
     *
     * <p>Called during FML's construction phase, long before the model loader
     * runs, so the disk read overlaps with mod loading instead of delaying it. If
     * the pipeline finishes early the models hit a warm cache; if it has not
     * finished by the time a model is needed, {@link #awaitPrepared} waits for it,
     * because by then the work is already in flight and waiting is cheaper than
     * loading everything the slow way.</p>
     */
    public void prepareAsync(final File configDir) {
        if (!isEnabled() || prepared) {
            return;
        }
        prepared = true;
        prepareFuture = EnvironmentFingerprint.prewarmAsync()
                                              .thenAcceptAsync(seed -> {
                    if (seed == null) {
                        StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] Environment fingerprint "
                                + "unavailable; running without the model cache this session.");
                        return;
                    }
                    loadedSeed = seed;
                    readCacheFile(configDir, seed);
                    // Clean up now rather than waiting for a write: a fully
                    // cached run never writes, yet it is exactly the run that
                    // should retire the previous environment's files.
                    pruneStaleFiles(configDir, seed);
                }, runnable -> {
                    // The fingerprint thread becomes the cache reader too, so the
                    // pipeline costs one background thread in total.
                    final Thread reader = new Thread(runnable, "StellarCore-VanillaModelCache-Read");
                    reader.setDaemon(true);
                    reader.start();
                });
    }

    /**
     * Block until {@link #prepareAsync} has finished, up to a generous cap.
     *
     * <p>Only called from the model loading path, where the pipeline has had the
     * entire mod loading phase to complete. If it is somehow still running, the
     * remaining work is a single file read, so waiting for it is cheaper than
     * abandoning the cache and letting thousands of models load one by one.</p>
     */
    public void awaitPrepared(final long millis) {
        final CompletableFuture<Void> future = prepareFuture;
        if (future == null) {
            return;
        }
        try {
            future.get(millis, TimeUnit.MILLISECONDS);
        } catch (Throwable t) {
            StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] Cache was not ready in time; "
                    + "models will load normally and the cache will be rebuilt.");
        }
    }

    /**
     * Recompute the fingerprint and reload the cache on the calling thread.
     *
     * <p>Used after a resource reload. A reload is an explicit, user-visible
     * event rather than a startup race, and the answer decides whether the cache
     * may be inherited, so it must be known before model loading resumes rather
     * than raced against it.</p>
     */
    public void prepareSync(final File configDir) {
        if (!isEnabled()) {
            return;
        }
        final long newSeed = EnvironmentFingerprint.recomputeNow();
        if (newSeed == 0L) {
            return;
        }
        loadedSeed = newSeed;
        prepared = true;
        prepareFuture = CompletableFuture.completedFuture(null);
        readCacheFile(configDir, newSeed);
        pruneStaleFiles(configDir, newSeed);
    }

    /** Whether the prepare pipeline has been started. */
    public boolean isPrepared() {
        return prepareFuture != null && prepareFuture.isDone();
    }

    /**
     * Read and decode the cache file for {@code seed}. Failures are logged and
     * leave the cache empty: a broken or missing file just means this session
     * loads models normally and rewrites the cache at the end.
     */
    private void readCacheFile(final File configDir, final long seed) {
        final File file = cacheFile(configDir, seed);
        if (!file.isFile()) {
            StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] No cache for environment {}, will populate.",
                    Long.toHexString(seed));
            return;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(file))))) {
            final VanillaModelDiskCacheFile.ReadResult result = VanillaModelDiskCacheFile.read(in, seed);
            if (result == null) {
                StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] Cache file {} does not match this "
                        + "environment, will regenerate.", file.getName());
                return;
            }
            diskView = result.entries;
            StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] Loaded {} vanilla model snapshots from {}.",
                    result.entries.size(), file.getName());
        } catch (Throwable t) {
            StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Failed to read {}; ignoring it.",
                    file.getAbsolutePath(), t);
        }
        readBlockstateFile(configDir, seed);
    }

    /**
     * Read and decode the blockstate cache for {@code seed}. Stored separately
     * from the model cache so a corrupt blockstate file cannot take the model
     * cache down with it; both share the environment fingerprint.
     */
    private void readBlockstateFile(final File configDir, final long seed) {
        final File file = blockstateFile(configDir, seed);
        if (!file.isFile()) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(file))))) {
            final BlockstateDiskCacheFile.ReadResult result = BlockstateDiskCacheFile.read(in, seed);
            if (result == null) {
                return;
            }
            blockstateView = result.entries;
            StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] Loaded {} blockstate snapshots from {}.",
                    result.entries.size(), file.getName());
        } catch (Throwable t) {
            StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Failed to read {}; ignoring it.",
                    file.getAbsolutePath(), t);
        }
    }

    /**
     * Write everything captured this run to disk on a background thread. Uses a
     * temp file plus an atomic move so an interrupted write can never leave a
     * half-written cache behind. No-op when nothing was captured.
     */
    public void saveAsync(final File configDir) {
        if (!isEnabled() || loadedSeed == 0L) {
            return;
        }
        final long seed = loadedSeed;
        final Map<ResourceLocation, VanillaModelSnapshot> models;
        final Map<ResourceLocation, BlockstateSnapshot> blockstates;
        synchronized (this) {
            if (live.isEmpty() && liveBlockstates.isEmpty()) {
                // Nothing to write, but retired environments may still need
                // cleaning up.
                pruneStaleFiles(configDir, seed);
                return;
            }
            models = new Object2ObjectOpenHashMap<>(live.size() * 2);
            models.putAll(live);
            blockstates = new Object2ObjectOpenHashMap<>(liveBlockstates.size() * 2);
            blockstates.putAll(liveBlockstates);
        }
        final Thread writer = new Thread(() -> {
            try {
                save(configDir, seed, models);
                saveBlockstates(configDir, seed, blockstates);
            } catch (Throwable t) {
                StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Failed to save cache", t);
            }
        }, "StellarCore-VanillaModelDiskCache-Save");
        writer.setDaemon(true);
        writer.start();
    }

    private void saveBlockstates(final File configDir,
                                 final long seed,
                                 final Map<ResourceLocation, BlockstateSnapshot> blockstates) throws IOException {
        if (blockstates.isEmpty()) {
            return;
        }
        final File file = blockstateFile(configDir, seed);
        final File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("cannot create config dir " + parent);
        }
        final File tmp = new File(parent, file.getName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                new GZIPOutputStream(new FileOutputStream(tmp))))) {
            BlockstateDiskCacheFile.write(out, seed, blockstates);
        }
        try {
            Files.move(tmp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException fallback) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] Wrote {} blockstate snapshots to {}.",
                blockstates.size(), file.getName());
    }

    private void save(final File configDir,
                      final long seed,
                      final Map<ResourceLocation, VanillaModelSnapshot> snapshot) throws IOException {
        final File file = cacheFile(configDir, seed);
        final File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("cannot create config dir " + parent);
        }
        final File tmp = new File(parent, file.getName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                new GZIPOutputStream(new FileOutputStream(tmp))))) {
            VanillaModelDiskCacheFile.write(out, seed, snapshot);
        }
        try {
            Files.move(tmp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException fallback) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] Wrote {} entries to {}.",
                snapshot.size(), file.getName());
        pruneStaleFiles(configDir, seed);
    }

    /**
     * Delete every cache file that does not belong to the current environment.
     *
     * <p>Called after reading the cache and after writing one, so the files left
     * on disk always describe exactly one environment — the one we are running
     * in. Called from the read path as well because a fully cached run never
     * writes anything, yet it is precisely the run that must retire the previous
     * environment's files.</p>
     */
    private void pruneStaleFiles(final File configDir, final long seed) {
        final File parent = resolveCacheDir(configDir);
        if (parent == null) {
            return;
        }
        final String keepModel = cacheFile(parent, seed).getName();
        final String keepBlockstate = blockstateFile(parent, seed).getName();
        final long now = System.currentTimeMillis();
        int removed = 0;

        for (final String prefix : CACHE_PREFIXES) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent.toPath(),
                    prefix + "*")) {
                for (final Path path : stream) {
                    final String name = path.getFileName().toString();
                    if (name.equals(keepModel) || name.equals(keepBlockstate)) {
                        continue;
                    }
                    if (removeIfExpired(path, name, now)) {
                        removed++;
                    }
                }
            } catch (Throwable ignored) {
                // A directory we cannot list is not worth failing over.
            }
        }

        if (removed > 0) {
            StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] Removed {} cache file(s) "
                    + "belonging to other environments.", removed);
        }
    }

    /**
     * Delete a file the cache owns if it is no longer wanted.
     *
     * <p>Cache files for any environment other than the current one are removed
     * outright: their name carries the fingerprint they were written for, so they
     * can never be valid here. Temp files get a short grace period instead,
     * because one may belong to a write this very run started in the background.
     * Anything else matching the prefix is left alone.</p>
     *
     * @return whether a file was actually deleted
     */
    private boolean removeIfExpired(final Path path, final String name, final long now) {
        try {
            if (name.endsWith(FILE_SUFFIX + ".tmp")) {
                if (Files.getLastModifiedTime(path).toMillis() >= now - STALE_TMP_AGE_MILLIS) {
                    return false;
                }
            } else if (!name.endsWith(FILE_SUFFIX)) {
                return false;
            }
            return Files.deleteIfExists(path);
        } catch (Throwable ignored) {
            // A file we cannot inspect or delete is not worth failing over.
            return false;
        }
    }

    @Nullable
    private File resolveCacheDir(final File configDir) {
        final File parent = configDir.isDirectory() ? configDir : configDir.getParentFile();
        return parent != null && parent.isDirectory() ? parent : null;
    }

    /**
     * Drop everything known about the current environment. Called when a resource
     * reload starts, because the reload may legitimately change which packs are
     * active; {@link #prepareSync} then re-reads the file for the newly computed
     * fingerprint, so a reload that changed nothing inherits the cache and one
     * that changed models does not.
     */
    public void onModelCacheCleared() {
        live.clear();
        liveBlockstates.clear();
        diskView = null;
        blockstateView = null;
        loadedSeed = 0L;
        prepareFuture = null;
        prepared = false;
    }

    // ------------------------------------------------------------------------
    // Hit path — no IO
    // ------------------------------------------------------------------------

    /**
     * Return the cached {@link ModelBlock} for {@code location}, or {@code null}
     * to let the caller run the normal loader.
     *
     * <p>Pure in-memory: a map lookup plus a Gson parse of the stored JSON. The
     * entry is dropped (so the normal loader re-captures it) if the stored JSON
     * no longer parses.</p>
     */
    @Nullable
    public ModelBlock tryBuildModelBlock(final ResourceLocation location) {
        final Map<ResourceLocation, VanillaModelSnapshot> view = diskView;
        if (view == null) {
            return null;
        }
        final ResourceLocation key = normalize(location);
        final VanillaModelSnapshot snapshot = view.get(key);
        if (snapshot == null) {
            return null;
        }
        try {
            final ModelBlock model = ModelBlock.deserialize(snapshot.modelJsonText());
            model.name = location.toString();
            return model;
        } catch (Throwable t) {
            StellarLog.LOG.debug("[StellarCore-VanillaModelDiskCache] Discarding unparsable entry {}: {}",
                    key, t.toString());
            view.remove(key, snapshot);
            return null;
        }
    }

    /**
     * Return the cached {@link ModelBlockAnimation} for {@code location}, or
     * {@code null} to let the caller run the normal loader.
     *
     * <p>When the snapshot recorded that this model has no armature, the shared
     * empty animation is returned, which is what Forge would have substituted
     * after its own failed lookup — without the resource pack probe.</p>
     */
    @Nullable
    public ModelBlockAnimation tryBuildAnimation(final ResourceLocation location) {
        final Map<ResourceLocation, VanillaModelSnapshot> view = diskView;
        if (view == null) {
            return null;
        }
        final ResourceLocation key = normalize(location);
        final VanillaModelSnapshot snapshot = view.get(key);
        if (snapshot == null) {
            return null;
        }
        if (!snapshot.hasArmature()) {
            return EMPTY_ANIMATION;
        }
        try {
            final ResourceLocation armatureLocation = armatureLocation(location);
            return ModelBlockAnimation.loadVanillaAnimation(
                    new InMemoryResourceManager(armatureLocation, snapshot.armatureJson), armatureLocation);
        } catch (Throwable t) {
            StellarLog.LOG.debug("[StellarCore-VanillaModelDiskCache] Discarding unparsable armature for {}: {}",
                    key, t.toString());
            view.remove(key, snapshot);
            return null;
        }
    }

    /**
     * Build the cached blockstate for {@code blockstateLocation}, or return
     * {@code null} to let the caller run the normal loader.
     *
     * <p>Every contributing part is parsed and combined exactly as
     * {@code ModelBlockDefinition(List)} would, in pack priority order. Because
     * the environment fingerprint already guarantees the pack list is unchanged,
     * no per-part identity check is needed.</p>
     */
    @Nullable
    public ModelBlockDefinition tryBuildBlockstate(final ResourceLocation blockstateLocation,
                                                   final ResourceLocation ownerLocation) {
        final Map<ResourceLocation, BlockstateSnapshot> view = blockstateView;
        if (view == null) {
            return null;
        }
        final BlockstateSnapshot snapshot = view.get(blockstateLocation);
        if (snapshot == null) {
            return null;
        }
        try {
            final List<ModelBlockDefinition> parts = new ArrayList<>(snapshot.parts.length);
            for (final byte[] part : snapshot.parts) {
                parts.add(ModelBlockDefinition.parseFromReader(
                        new StringReader(new String(part, StandardCharsets.UTF_8)), ownerLocation));
            }
            return new ModelBlockDefinition(parts);
        } catch (Throwable t) {
            StellarLog.LOG.debug("[StellarCore-VanillaModelDiskCache] Discarding unparsable blockstate {}: {}",
                    blockstateLocation, t.toString());
            view.remove(blockstateLocation, snapshot);
            return null;
        }
    }

    /**
     * Record the parts a blockstate was assembled from. Parts must be in pack
     * priority order, matching what {@code getAllResources} returned.
     */
    public void captureBlockstate(final ResourceLocation blockstateLocation, final byte[][] parts) {
        if (!isEnabled() || parts == null || parts.length == 0) {
            return;
        }
        liveBlockstates.put(blockstateLocation, new BlockstateSnapshot(blockstateLocation, parts));
    }

    /**
     * Whether this location is currently served from the cache. Used by the
     * capture injection to avoid re-reading a model it just served.
     */
    public boolean knows(final ResourceLocation location) {
        final Map<ResourceLocation, VanillaModelSnapshot> view = diskView;
        return view != null && view.containsKey(normalize(location));
    }

    // ------------------------------------------------------------------------
    // Capture path
    // ------------------------------------------------------------------------

    /**
     * Record the raw JSON backing a model the normal loader just produced. Runs
     * once per model per environment (a hit short-circuits before this is
     * reached), so the extra read is paid only when the cache is being built or
     * repaired. Failures are swallowed: caching is best-effort and must never
     * break model loading.
     */
    public void captureFrom(final ResourceLocation location, final IResourceManager manager) {
        if (!isEnabled() || manager == null) {
            return;
        }
        try {
            final ResourceLocation key = normalize(location);
            final byte[] modelJson = readResourceBytes(manager, modelJsonLocation(location));
            if (modelJson == null || modelJson.length == 0) {
                return;
            }
            final byte[] armatureJson = readResourceBytes(manager, armatureLocation(location));
            live.put(key, new VanillaModelSnapshot(key, modelJson, armatureJson));
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------------
    // Location helpers
    // ------------------------------------------------------------------------

    /**
     * Cache keys drop Forge's {@code "models/"} prefix, because that is the path
     * shape a model JSON actually has inside a resource pack. Locations reach us
     * both with and without the prefix depending on the call site.
     */
    private static ResourceLocation normalize(final ResourceLocation location) {
        final String path = location.getPath();
        if (path.startsWith("models/")) {
            return new ResourceLocation(location.getNamespace(), path.substring("models/".length()));
        }
        return location;
    }

    private static ResourceLocation modelJsonLocation(final ResourceLocation location) {
        final ResourceLocation normalized = normalize(location);
        return new ResourceLocation(normalized.getNamespace(), "models/" + normalized.getPath() + ".json");
    }

    private static ResourceLocation armatureLocation(final ResourceLocation location) {
        final ResourceLocation normalized = normalize(location);
        return new ResourceLocation(normalized.getNamespace(), "armatures/" + normalized.getPath() + ".json");
    }

    private static File cacheFile(final File configDir, final long seed) {
        return new File(configDir, FILE_PREFIX + Long.toHexString(seed) + FILE_SUFFIX);
    }

    private static File blockstateFile(final File configDir, final long seed) {
        return new File(configDir, BLOCKSTATE_PREFIX + Long.toHexString(seed) + FILE_SUFFIX);
    }

    // ------------------------------------------------------------------------
    // IO helpers
    // ------------------------------------------------------------------------

    @Nullable
    private static byte[] readResourceBytes(final IResourceManager manager, final ResourceLocation location) {
        try (IResource resource = manager.getResource(location);
             InputStream in = resource.getInputStream()) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(8192, in.available()));
            final byte[] buffer = new byte[16384];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (Throwable missing) {
            return null;
        }
    }

    /**
     * Serves a single already-read resource back to code that expects an
     * {@link IResourceManager}. Used only to feed the stock animation parser,
     * replacing the resource pack probe it would otherwise perform.
     */
    private static final class InMemoryResourceManager implements IResourceManager {
        private final ResourceLocation only;
        private final byte[] bytes;

        InMemoryResourceManager(final ResourceLocation only, final byte[] bytes) {
            this.only = only;
            this.bytes = bytes;
        }

        @Override
        public Set<String> getResourceDomains() {
            return Collections.singleton(only.getNamespace());
        }

        @Override
        public IResource getResource(final ResourceLocation location) throws IOException {
            if (!location.equals(only)) {
                throw new FileNotFoundException(location.toString());
            }
            final ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
            return new IResource() {
                @Override
                public ResourceLocation getResourceLocation() {
                    return location;
                }

                @Override
                public InputStream getInputStream() {
                    return stream;
                }

                @Override
                public boolean hasMetadata() {
                    return false;
                }

                @Nullable
                @Override
                public <T extends IMetadataSection> T getMetadata(final String section) {
                    return null;
                }

                @Override
                public String getResourcePackName() {
                    return "stellarcore-model-cache";
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public List<IResource> getAllResources(final ResourceLocation location) throws IOException {
            return Collections.singletonList(getResource(location));
        }
    }
}
