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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BooleanSupplier;
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
 * <h2>Capture</h2>
 * A snapshot holds the bytes the model loader itself read, tapped as it read them
 * (see {@link ModelCapture}). Reading each file a second time afterwards would
 * double the resource lookups on the model-loading critical path, and on a
 * machine where that second lookup comes back empty it would leave the cache
 * silently empty forever — so the second read exists only as a fallback for when
 * a tap could not be installed.</p>
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
    private Map<ResourceLocation, VanillaModelSnapshot> live = new ConcurrentHashMap<>();

    /** Blockstates captured during this run, persisted alongside the models. */
    private Map<ResourceLocation, BlockstateSnapshot> liveBlockstates = new ConcurrentHashMap<>();

    /**
     * Vanilla-format model loads observed this session, and where their bytes
     * came from. The capture path is best-effort and swallows its own failures,
     * so these counters are the only way {@link #saveAsync} can tell "nothing to
     * save" apart from "capture is broken" — and, when it is broken, which half
     * of it broke.
     */
    private final LongAdder captureAttempts = new LongAdder();

    /** Captures whose bytes came from the loader's own read. */
    private final LongAdder captureTapped = new LongAdder();

    /** Captures whose bytes had to be read again, the tap being unavailable. */
    private final LongAdder captureReread = new LongAdder();

    /** Captures that produced nothing: no read yielded the model JSON. */
    private final LongAdder captureMisses = new LongAdder();

    /**
     * The fingerprint the captured snapshots were taken under. Captures may
     * survive several resource reloads (mods reload resources for their own
     * reasons without touching models), so the capture maps cannot be wiped
     * whenever a reload starts; they are dropped only when a reload recomputes
     * a different fingerprint, and included in a save only when the tag still
     * matches the seed being written.
     */
    private volatile long captureSeed = 0L;

    /** Snapshots read from disk for the current environment; the hit source. */
    private volatile Map<ResourceLocation, VanillaModelSnapshot> diskView = null;

    /** Blockstates read from disk for the current environment; the hit source. */
    private volatile Map<ResourceLocation, BlockstateSnapshot> blockstateView = null;

    private volatile long loadedSeed = 0L;

    /**
     * Whether this session's fingerprint has been corroborated by an actual
     * cache file. The pack list grows while mods load, so a fingerprint computed
     * early can name an environment that never had a cache; pruning on such a
     * guess would delete the very files the next (correct) fingerprint needs.
     * Only a successful read or write proves the environment is real.
     */
    private volatile boolean environmentConfirmed = false;

    /** The fingerprint-then-read pipeline started by {@link #prepareAsync}. */
    private volatile CompletableFuture<Void> prepareFuture = null;
    private volatile boolean prepared = false;

    /** All read/merge/replace operations run in submission order. */
    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(runnable -> {
        final Thread thread = new Thread(runnable, "StellarCore-VanillaModelDiskCache-Save");
        thread.setDaemon(true);
        return thread;
    });
    private CompletableFuture<Void> saveFuture = CompletableFuture.completedFuture(null);
    private boolean saveScheduled;
    /** Only atomic file replacement and generation changes use this lock, never compression. */
    private final Object commitLock = new Object();
    private SaveRequest pendingRequest;
    private SaveBatch inFlight;
    private long commitRevision;
    private long viewGeneration;
    private File lastSaveDir;
    private long reloadGeneration;
    private long rejectionSeed;
    private Set<ResourceLocation> rejectedModels = new HashSet<>();
    private Set<ResourceLocation> rejectedBlockstates = new HashSet<>();

    private final BooleanSupplier enabled;

    private VanillaModelDiskCache() {
        this(() -> StellarCoreConfig.PERFORMANCE.forge.vanillaModelDiskCache, true);
    }

    VanillaModelDiskCache(final BooleanSupplier enabled) {
        this(enabled, false);
    }

    private VanillaModelDiskCache(final BooleanSupplier enabled, final boolean installShutdownHook) {
        this.enabled = enabled;
        if (installShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownWriter,
                    "StellarCore-VanillaModelDiskCache-Shutdown"));
        }
    }

    void shutdownWriter() {
        synchronized (this) {
            if (lastSaveDir != null && !saveExecutor.isShutdown()) {
                saveAsync(lastSaveDir);
            }
            saveExecutor.shutdown();
        }
        try {
            if (!saveExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Timed out waiting for cache save; "
                        + "the previously committed files remain usable.");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    synchronized CompletableFuture<Void> pendingSave() {
        return saveFuture;
    }

    public boolean isEnabled() {
        return enabled.getAsBoolean();
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
    public synchronized void prepareAsync(final File configDir) {
        if (!isEnabled() || prepared) {
            return;
        }
        prepared = true;
        final long generation = reloadGeneration;
        prepareFuture = EnvironmentFingerprint.prewarmAsync()
                                              .thenAcceptAsync(seed -> {
                    if (seed == null) {
                        StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] Environment fingerprint "
                                + "unavailable; running without the model cache this session.");
                        return;
                    }
                    prepareEnvironment(configDir, seed, generation);
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
        prepareSync(configDir, EnvironmentFingerprint.recomputeNow());
    }

    // Separate fingerprint computation from the lifecycle for headless regression tests.
    void prepareSync(final File configDir, final long newSeed) {
        prepareEnvironment(configDir, newSeed, -1L);
    }

    private void prepareEnvironment(final File configDir, final long newSeed, final long expectedGeneration) {
        final long generation;
        final long revision;
        final long views;
        synchronized (commitLock) {
            synchronized (this) {
                if (expectedGeneration != -1L && expectedGeneration != reloadGeneration) {
                    return;
                }
                generation = ++reloadGeneration;
                revision = commitRevision;
                views = viewGeneration;
                diskView = null;
                blockstateView = null;
                environmentConfirmed = false;
                if (newSeed == 0L || (captureSeed != 0L && captureSeed != newSeed)) {
                    live.clear();
                    liveBlockstates.clear();
                    captureSeed = 0L;
                }
                if (rejectionSeed != newSeed) {
                    rejectedModels.clear();
                    rejectedBlockstates.clear();
                    rejectionSeed = newSeed;
                }
                loadedSeed = newSeed;
                prepared = true;
            }
        }
        if (newSeed != 0L) {
            final Map<ResourceLocation, VanillaModelSnapshot> models = readModels(configDir, newSeed);
            final Map<ResourceLocation, BlockstateSnapshot> blockstates = readBlockstates(configDir, newSeed);
            synchronized (this) {
                if (generation != reloadGeneration) {
                    return;
                }
                if (revision != commitRevision || views != viewGeneration) {
                    prepareFuture = CompletableFuture.completedFuture(null);
                    return;
                }
                if (models != null) {
                    models.keySet().removeAll(rejectedModels);
                }
                if (blockstates != null) {
                    blockstates.keySet().removeAll(rejectedBlockstates);
                }
                diskView = models;
                blockstateView = blockstates;
                environmentConfirmed = models != null || blockstates != null;
            }
            if (models != null) {
                StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] Loaded {} vanilla model snapshots from {}.",
                        models.size(), cacheFile(configDir, newSeed).getName());
            }
            if (blockstates != null) {
                StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] Loaded {} blockstate snapshots from {}.",
                        blockstates.size(), blockstateFile(configDir, newSeed).getName());
            }
            pruneStaleFiles(configDir, newSeed, generation);
        }
        synchronized (this) {
            if (generation == reloadGeneration) {
                prepareFuture = CompletableFuture.completedFuture(null);
            }
        }
    }

    /** Whether the prepare pipeline has finished. */
    public boolean isPrepared() {
        return prepareFuture != null && prepareFuture.isDone();
    }

    @Nullable
    private Map<ResourceLocation, VanillaModelSnapshot> readModels(final File configDir, final long seed) {
        final File file = cacheFile(configDir, seed);
        if (!file.isFile()) {
            return null;
        }
        try (InputStream raw = new FileInputStream(file);
             DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(raw)))) {
            final VanillaModelDiskCacheFile.ReadResult result = VanillaModelDiskCacheFile.read(in, seed);
            if (result != null) {
                return result.entries;
            }
        } catch (IOException | RuntimeException t) {
            StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Failed to read {}; ignoring it.", file, t);
        }
        return null;
    }

    @Nullable
    private Map<ResourceLocation, BlockstateSnapshot> readBlockstates(final File configDir, final long seed) {
        final File file = blockstateFile(configDir, seed);
        if (!file.isFile()) {
            return null;
        }
        try (InputStream raw = new FileInputStream(file);
             DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(raw)))) {
            final BlockstateDiskCacheFile.ReadResult result = BlockstateDiskCacheFile.read(in, seed);
            if (result != null) {
                return result.entries;
            }
        } catch (IOException | RuntimeException t) {
            StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Failed to read {}; ignoring it.", file, t);
        }
        return null;
    }

    /**
     * Write everything captured this run to disk on a background thread. Uses a
     * temp file plus an atomic move so an interrupted write can never leave a
     * half-written cache behind. No-op when nothing was captured.
     */
    public synchronized void saveAsync(final File configDir) {
        if (!isEnabled() || loadedSeed == 0L || saveExecutor.isShutdown()
                || (live.isEmpty() && liveBlockstates.isEmpty() && rejectedModels.isEmpty()
                    && rejectedBlockstates.isEmpty() && inFlight == null)) {
            return;
        }
        lastSaveDir = configDir;
        pendingRequest = new SaveRequest(configDir, loadedSeed, reloadGeneration);
        if (saveScheduled) {
            return;
        }
        saveScheduled = true;
        try {
            saveFuture = CompletableFuture.runAsync(this::drainSaves, saveExecutor);
        } catch (RejectedExecutionException shuttingDown) {
            saveScheduled = false;
            pendingRequest = null;
        }
    }

    private void drainSaves() {
        while (true) {
            final SaveBatch batch;
            synchronized (this) {
                final SaveRequest request = pendingRequest;
                pendingRequest = null;
                if (request == null) {
                    saveScheduled = false;
                    return;
                }
                if (request.seed != loadedSeed || request.generation != reloadGeneration) {
                    continue;
                }
                // Detach once, regardless of how often F3+T requests another save.
                batch = new SaveBatch(request, live, liveBlockstates, rejectedModels, rejectedBlockstates);
                inFlight = batch;
                live = new ConcurrentHashMap<>();
                liveBlockstates = new ConcurrentHashMap<>();
                rejectedModels = new HashSet<>();
                rejectedBlockstates = new HashSet<>();
            }
            boolean modelsSaved = false;
            boolean blockstatesSaved = false;
            try {
                Map<ResourceLocation, VanillaModelSnapshot> models = readModels(batch.request.dir, batch.request.seed);
                if (models == null) {
                    models = new Object2ObjectOpenHashMap<>();
                }
                models.keySet().removeAll(batch.rejectedModels);
                models.putAll(batch.models);
                modelsSaved = saveModels(batch.request, models);

                Map<ResourceLocation, BlockstateSnapshot> blockstates = readBlockstates(batch.request.dir, batch.request.seed);
                if (blockstates == null) {
                    blockstates = new Object2ObjectOpenHashMap<>();
                }
                blockstates.keySet().removeAll(batch.rejectedBlockstates);
                blockstates.putAll(batch.blockstates);
                blockstatesSaved = saveBlockstates(batch.request, blockstates);
                if (modelsSaved && blockstatesSaved) {
                    pruneStaleFiles(batch.request.dir, batch.request.seed, batch.request.generation);
                }
            } catch (Throwable t) {
                StellarLog.LOG.warn("[StellarCore-VanillaModelDiskCache] Failed to save cache", t);
            } finally {
                synchronized (this) {
                    // Failed/obsolete writes remain retryable only in the same resource environment.
                    // New captures win over the detached older delta for a shared key.
                    if (loadedSeed == batch.request.seed || (loadedSeed == 0L
                            && (captureSeed == batch.request.seed || rejectionSeed == batch.request.seed))) {
                        if (!modelsSaved) {
                            batch.models.forEach(live::putIfAbsent);
                            rejectedModels.addAll(batch.rejectedModels);
                        }
                        if (!blockstatesSaved) {
                            batch.blockstates.forEach(liveBlockstates::putIfAbsent);
                            rejectedBlockstates.addAll(batch.rejectedBlockstates);
                        }
                    }
                    inFlight = null;
                }
            }
        }
    }

    private boolean saveModels(final SaveRequest request,
                               final Map<ResourceLocation, VanillaModelSnapshot> models) throws IOException {
        final File file = cacheFile(request.dir, request.seed);
        final File tmp = temporaryFile(file);
        try {
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                    new GZIPOutputStream(new FileOutputStream(tmp))))) {
                VanillaModelDiskCacheFile.write(out, request.seed, models);
            }
            if (!commit(request, tmp, file)) {
                return false;
            }
            StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] Wrote {} entries to {}.",
                    models.size(), file.getName());
            return true;
        } finally {
            if (tmp.isFile()) {
                Files.deleteIfExists(tmp.toPath());
            }
        }
    }

    private boolean saveBlockstates(final SaveRequest request,
                                    final Map<ResourceLocation, BlockstateSnapshot> blockstates) throws IOException {
        final File file = blockstateFile(request.dir, request.seed);
        final File tmp = temporaryFile(file);
        try {
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                    new GZIPOutputStream(new FileOutputStream(tmp))))) {
                BlockstateDiskCacheFile.write(out, request.seed, blockstates);
            }
            if (!commit(request, tmp, file)) {
                return false;
            }
            StellarLog.LOG.info("[StellarCore-VanillaModelDiskCache] Wrote {} blockstate snapshots to {}.",
                    blockstates.size(), file.getName());
            return true;
        } finally {
            if (tmp.isFile()) {
                Files.deleteIfExists(tmp.toPath());
            }
        }
    }

    private static File temporaryFile(final File file) throws IOException {
        final File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("cannot create config dir " + parent);
        }
        return new File(parent, file.getName() + ".tmp");
    }

    private boolean commit(final SaveRequest request, final File tmp, final File file) throws IOException {
        synchronized (commitLock) {
            synchronized (this) {
                if (request.generation != reloadGeneration || request.seed != loadedSeed) {
                    return false;
                }
            }
            // The only file IO serialized with invalidation: one atomic replacement.
            try {
                Files.move(tmp.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException fallback) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            synchronized (this) {
                environmentConfirmed = true;
                commitRevision++;
            }
            return true;
        }
    }

    private static final class SaveRequest {
        final File dir;
        final long seed;
        final long generation;

        SaveRequest(final File dir, final long seed, final long generation) {
            this.dir = dir;
            this.seed = seed;
            this.generation = generation;
        }
    }

    private static final class SaveBatch {
        final SaveRequest request;
        final Map<ResourceLocation, VanillaModelSnapshot> models;
        final Map<ResourceLocation, BlockstateSnapshot> blockstates;
        final Set<ResourceLocation> rejectedModels;
        final Set<ResourceLocation> rejectedBlockstates;

        SaveBatch(final SaveRequest request, final Map<ResourceLocation, VanillaModelSnapshot> models,
                  final Map<ResourceLocation, BlockstateSnapshot> blockstates,
                  final Set<ResourceLocation> rejectedModels, final Set<ResourceLocation> rejectedBlockstates) {
            this.request = request;
            this.models = models;
            this.blockstates = blockstates;
            this.rejectedModels = rejectedModels;
            this.rejectedBlockstates = rejectedBlockstates;
        }
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
    private void pruneStaleFiles(final File configDir, final long seed, final long generation) {
        if (!environmentConfirmed) {
            // Nothing has corroborated this fingerprint, so files named after
            // other fingerprints are not known to be foreign. Leaving them costs
            // disk; deleting them costs a full model reload on the next run.
            return;
        }
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
                    synchronized (commitLock) {
                        synchronized (this) {
                            if (generation != reloadGeneration || seed != loadedSeed || !environmentConfirmed) {
                                return;
                            }
                        }
                        if (removeIfExpired(path, name, now)) {
                            removed++;
                        }
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
     * Drop everything known about the current environment's *views*. Called when
     * a resource reload starts, because the reload may legitimately change which
     * packs are active; {@link #prepareSync} then re-reads the file for the newly
     * computed fingerprint, so a reload that changed nothing inherits the cache
     * and one that changed models does not.
     *
     * <p>The capture maps are deliberately not touched here. Mods reload
     * resources for their own reasons after the model loader has run, and a
     * reload whose fingerprint ends up unchanged must not throw away the
     * snapshots this session captured — on some machines such a reload lands
     * between capture and save, which used to cost the entire cache. Whether
     * the captures still belong to the current environment is decided in
     * {@link #prepareSync}, where the fresh fingerprint is known.</p>
     */
    public void onModelCacheCleared() {
        synchronized (commitLock) {
            synchronized (this) {
                reloadGeneration++;
                environmentConfirmed = false;
                diskView = null;
                blockstateView = null;
                loadedSeed = 0L;
                prepareFuture = null;
                prepared = false;
            }
        }
    }

    public synchronized void releaseLoadedViews() {
        viewGeneration++;
        diskView = null;
        blockstateView = null;
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
            synchronized (this) {
                if (view == diskView && view.remove(key, snapshot)) {
                    rejectionSeed = loadedSeed;
                    rejectedModels.add(key);
                }
            }
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
            synchronized (this) {
                if (view == diskView && view.remove(key, snapshot)) {
                    rejectionSeed = loadedSeed;
                    rejectedModels.add(key);
                }
            }
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
            synchronized (this) {
                if (view == blockstateView && view.remove(blockstateLocation, snapshot)) {
                    rejectionSeed = loadedSeed;
                    rejectedBlockstates.add(blockstateLocation);
                }
            }
            return null;
        }
    }

    /**
     * Record the parts a blockstate was assembled from. Parts must be in pack
     * priority order, matching what {@code getAllResources} returned.
     */
    public synchronized void captureBlockstate(final ResourceLocation blockstateLocation, final byte[][] parts) {
        if (!isEnabled() || parts == null || parts.length == 0) {
            return;
        }
        if (loadedSeed == 0L) {
            return;
        }
        captureSeed = loadedSeed;
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
     * reached), so it only costs anything while the cache is being built or
     * repaired. Failures are swallowed: caching is best-effort and must never
     * break model loading.
     *
     * <p>Both files come from the loader's own reads when the taps are in place,
     * which is free. The files are read here only when a tap is unavailable, so a
     * machine whose taps cannot be installed keeps the old behaviour instead of
     * losing the cache.</p>
     */
    public void captureFrom(final ResourceLocation location, final IResourceManager manager) {
        if (!isEnabled()) {
            return;
        }
        final long generation;
        final long seed;
        synchronized (this) {
            generation = reloadGeneration;
            seed = loadedSeed;
        }
        if (seed == 0L) {
            return;
        }
        captureAttempts.increment();
        try {
            final ResourceLocation key = normalize(location);
            final byte[] tappedModel = ModelCapture.modelJson();
            final byte[] modelJson = tappedModel != null ? tappedModel
                    : manager == null ? null : readResourceBytes(manager, modelJsonLocation(location));
            if (modelJson == null || modelJson.length == 0) {
                captureMisses.increment();
                return;
            }
            if (tappedModel != null) {
                captureTapped.increment();
            } else {
                captureReread.increment();
            }
            final VanillaModelSnapshot snapshot = new VanillaModelSnapshot(key, modelJson, armatureJson(location, manager));
            synchronized (this) {
                if (generation == reloadGeneration && seed == loadedSeed) {
                    captureSeed = seed;
                    live.put(key, snapshot);
                }
            }
        } catch (Throwable ignored) {
            captureMisses.increment();
        }
    }

    /**
     * The armature JSON for {@code location}, or {@code null} when the model has
     * none.
     *
     * <p>Normally answered by the tap on Forge's own armature lookup, which also
     * reports the case a tap cannot see any other way: the lookup failed with
     * "file not found", so the model demonstrably has no armature. Without a tap
     * the file is read here, as it always was — a missing armature then records
     * as absent, exactly as before.</p>
     */
    @Nullable
    private static byte[] armatureJson(final ResourceLocation location, final IResourceManager manager) {
        final byte[] tapped = ModelCapture.armatureJson();
        if (tapped != null || ModelCapture.armatureAbsent()) {
            return tapped;
        }
        return manager == null ? null : readResourceBytes(manager, armatureLocation(location));
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
