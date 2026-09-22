package github.kasuminova.stellarcore.client.resource;

import github.kasuminova.stellarcore.common.util.StellarEnvironment;
import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingIdentityHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Complete entry-name index for archive-backed resource packs.
 *
 * <p>A zip archive cannot gain entries while it is mounted as a resource pack, so a finished index
 * answers both hits and misses authoritatively and lets {@code hasResourceName} skip the monitor
 * inside {@link ZipFile#getEntry(String)}. Lookups performed before the asynchronous scan completes
 * report {@link #UNKNOWN} and fall through to the live archive.</p>
 */
public final class ZipEntryIndex {

    public static final int UNKNOWN = 0;
    public static final int ABSENT = 1;
    public static final int PRESENT = 2;

    private static final boolean CASE_INSENSITIVE = isWindows();
    private static final NonBlockingHashMap<String, Index> INDEXES = new NonBlockingHashMap<>();
    private static final NonBlockingIdentityHashMap<File, String> KEYS = new NonBlockingIdentityHashMap<>();
    private static final AtomicLong GENERATION = new AtomicLong();
    private static final int MAX_SCAN_THREADS = 4;

    private static volatile ExecutorService executor;

    private ZipEntryIndex() {
    }

    public static void clear() {
        GENERATION.incrementAndGet();
        INDEXES.clear();
        KEYS.clear();
    }

    public static void invalidate(@Nullable final File archive) {
        if (archive != null) {
            INDEXES.remove(key(archive));
        }
    }

    public static void prewarmAsync(@Nullable final File archive, final Callable<ZipFile> opener) {
        if (archive == null || !archive.isFile()) {
            return;
        }
        final String key = key(archive);
        final Index created = new Index(GENERATION.get());
        if (INDEXES.putIfAbsent(key, created) != null) {
            return;
        }
        CompletableFuture.runAsync(() -> created.scan(key, archive, opener), executor());
    }

    public static int lookup(@Nullable final File archive, @Nullable final String name) {
        if (INDEXES.isEmpty() || archive == null || !isIndexable(name)) {
            return UNKNOWN;
        }
        final Index index = INDEXES.get(key(archive));
        if (index == null || index.generation != GENERATION.get()) {
            return UNKNOWN;
        }
        final ObjectOpenHashSet<String> names = index.names;
        if (names == null) {
            return UNKNOWN;
        }
        return names.contains(name) ? PRESENT : ABSENT;
    }

    public static Set<String> lookupResourceDomains(@Nullable final File archive) {
        if (archive == null) {
            return null;
        }
        final Index index = INDEXES.get(key(archive));
        if (index == null || index.generation != GENERATION.get()) {
            return null;
        }
        final ObjectOpenHashSet<String> domains = index.resourceDomains;
        return domains == null ? null : Collections.unmodifiableSet(domains);
    }

    public static Set<String> lookupInvalidResourceDomains(@Nullable final File archive) {
        if (archive == null) {
            return null;
        }
        final Index index = INDEXES.get(key(archive));
        if (index == null || index.generation != GENERATION.get()) {
            return null;
        }
        final ObjectOpenHashSet<String> domains = index.invalidResourceDomains;
        return domains == null ? null : Collections.unmodifiableSet(domains);
    }

    private static boolean isIndexable(@Nullable final String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return name.startsWith("assets/") || name.indexOf('/') < 0;
    }

    private static ExecutorService executor() {
        ExecutorService current = executor;
        if (current != null) {
            return current;
        }
        synchronized (ZipEntryIndex.class) {
            current = executor;
            if (current != null) {
                return current;
            }
            final int threads = Math.max(1, Math.min(MAX_SCAN_THREADS, StellarEnvironment.getConcurrency()));
            final AtomicInteger threadId = new AtomicInteger();
            final ThreadFactory threadFactory = runnable -> {
                final Thread thread = new Thread(runnable);
                thread.setName("StellarCore-ZipEntryIndex-" + threadId.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            };
            current = Executors.newFixedThreadPool(threads, threadFactory);
            executor = current;
            return current;
        }
    }

    /**
     * Returns the index key of one archive, computing it once per archive instance.
     *
     * <p>Every lookup asks for this key, and deriving it walks the archive's path through
     * {@link File#getAbsolutePath()} and a case fold, which allocates two strings per call. Packs hand back the
     * same {@link File}, so one identity-keyed entry serves all lookups; an archive seen as a new instance simply
     * computes its key again.</p>
     *
     * @param archive archive to describe
     * @return normalised absolute path used as the index key
     */
    private static String key(final File archive) {
        final String cached = KEYS.get(archive);
        if (cached != null) {
            return cached;
        }
        final String path = archive.getAbsolutePath();
        final String computed = CASE_INSENSITIVE ? path.toLowerCase(Locale.ROOT) : path;
        KEYS.put(archive, computed);
        return computed;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static final class Index {
        private final long generation;

        private volatile ObjectOpenHashSet<String> names;
        private volatile ObjectOpenHashSet<String> resourceDomains;
        private volatile ObjectOpenHashSet<String> invalidResourceDomains;

        private Index(final long generation) {
            this.generation = generation;
        }

        private boolean isStale() {
            return generation != GENERATION.get();
        }

        private void scan(final String key, final File archive, final Callable<ZipFile> opener) {
            if (isStale()) {
                INDEXES.remove(key, this);
                return;
            }

            final ObjectOpenHashSet<String> collected;
            final ObjectOpenHashSet<String> collectedDomains;
            final ObjectOpenHashSet<String> collectedInvalidDomains;
            try {
                @SuppressWarnings("resource") final ZipFile zipFile = opener.call();
                collected = new ObjectOpenHashSet<>(Math.max(16, zipFile.size()));
                collectedDomains = new ObjectOpenHashSet<>();
                collectedInvalidDomains = new ObjectOpenHashSet<>();
                final Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    final String name = entries.nextElement().getName();
                    if (name.isEmpty()) {
                        continue;
                    }
                    final boolean directory = name.charAt(name.length() - 1) == '/';
                    final String bare = directory ? name.substring(0, name.length() - 1) : name;
                    if (bare.startsWith("assets/")) {
                        final int namespaceStart = "assets/".length();
                        final int namespaceEnd = bare.indexOf('/', namespaceStart);
                        final String namespace = namespaceEnd < 0
                            ? bare.substring(namespaceStart)
                            : bare.substring(namespaceStart, namespaceEnd);
                        if (!namespace.isEmpty()) {
                            if (namespace.equals(namespace.toLowerCase(Locale.ROOT))) {
                                collectedDomains.add(namespace);
                            } else {
                                collectedInvalidDomains.add(namespace);
                            }
                        }
                    }
                    if (!isIndexable(bare)) {
                        continue;
                    }
                    collected.add(name);
                    if (directory) {
                        collected.add(bare);
                    }
                }
            } catch (Throwable failure) {
                StellarLog.LOG.warn(
                    "[StellarCore-ZipEntryIndex] Unable to index {}, falling back to live lookups.",
                    archive.getName(), failure
                );
                INDEXES.remove(key, this);
                return;
            }

            collected.trim();
            collectedDomains.trim();
            collectedInvalidDomains.trim();
            if (isStale()) {
                INDEXES.remove(key, this);
                return;
            }
            this.names = collected;
            this.resourceDomains = collectedDomains;
            this.invalidResourceDomains = collectedInvalidDomains;
        }
    }
}
