package github.kasuminova.stellarcore.client.resource;

import github.kasuminova.stellarcore.common.util.StellarEnvironment;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A minimal existence index for directory trees.
 *
 * <p>This is mainly used to avoid calling {@link File#exists()} / {@link File#isFile()} repeatedly
 * on Windows/NTFS during model/texture loading.
 */
public final class DirectoryPathIndex {

    private static final boolean CASE_INSENSITIVE = isWindows();

    private static final ConcurrentHashMap<String, Index> INDEXES = new ConcurrentHashMap<>();

    private static final int MAX_SCAN_THREADS = 4;
    private static volatile int executorThreads = 0;
    private static volatile ExecutorService executor;

    private DirectoryPathIndex() {
    }

    public static void clear() {
        INDEXES.clear();
    }

    public static void prewarmAsync(@Nullable final File rootDirectory) {
        if (rootDirectory == null) {
            return;
        }
        INDEXES.computeIfAbsent(normalizeKey(rootDirectory), key -> new Index(rootDirectory)).ensureInitializedAsync();
    }

    /**
     * Non-blocking existence check.
     *
     * @return {@code Boolean.TRUE}/{@code Boolean.FALSE} if the index is ready;
     * {@code null} if the index is not initialized yet.
     */
    @Nullable
    public static Boolean tryContains(@Nullable final File rootDirectory, @Nullable final String relativePath) {
        if (rootDirectory == null || relativePath == null || relativePath.isEmpty()) {
            return Boolean.FALSE;
        }
        final Index index = INDEXES.get(normalizeKey(rootDirectory));
        if (index == null) {
            return null;
        }
        return index.tryContains(relativePath);
    }

    private static Executor executor() {
        ExecutorService current = executor;
        if (current != null) {
            return current;
        }
        synchronized (DirectoryPathIndex.class) {
            current = executor;
            if (current != null) {
                return current;
            }
            final int concurrency = StellarEnvironment.getConcurrency();
            final int threads = Math.max(1, Math.min(MAX_SCAN_THREADS, concurrency));
            executorThreads = threads;
            final AtomicInteger threadId = new AtomicInteger(0);
            final ThreadFactory factory = runnable -> {
                final Thread thread = new Thread(runnable);
                thread.setName("StellarCore-DirectoryPathIndex-" + threadId.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            };
            current = Executors.newFixedThreadPool(threads, factory);
            executor = current;
            return current;
        }
    }

    private static int executorThreads() {
        if (executorThreads <= 0) {
            executor();
        }
        return Math.max(1, executorThreads);
    }

    private static final class Index {
        private final File root;

        private volatile boolean initialized = false;
        private volatile CompletableFuture<Void> initFuture;
        private volatile Set<String> paths = Collections.emptySet();

        private Index(final File root) {
            this.root = root;
        }

        @Nullable
        private Boolean tryContains(final String relativePath) {
            if (!initialized) {
                ensureInitializedAsync();
                return null;
            }
            return paths.contains(normalizePath(relativePath)) ? Boolean.TRUE : Boolean.FALSE;
        }

        private void ensureInitializedAsync() {
            if (initialized) {
                return;
            }
            final CompletableFuture<Void> current = initFuture;
            if (current != null) {
                return;
            }
            synchronized (this) {
                if (initialized || initFuture != null) {
                    return;
                }
                initFuture = CompletableFuture.runAsync(() -> {
                    try {
                        init();
                    } catch (Throwable ignored) {
                        paths = Collections.emptySet();
                    } finally {
                        initialized = true;
                    }
                }, DirectoryPathIndex.executor());
            }
        }

        private void init() {
            final Set<String> found = scan(root);
            this.paths = found.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(found);
        }

        private Set<String> scan(final File rootDir) {
            if (rootDir == null) {
                return Collections.emptySet();
            }

            final java.util.HashSet<String> found = new java.util.HashSet<>();
            final ArrayDeque<DirFrame> stack = new ArrayDeque<>();
            stack.push(new DirFrame(rootDir, ""));

            while (!stack.isEmpty()) {
                final DirFrame frame = stack.pop();
                final File dir = frame.dir;
                final String prefix = frame.prefix;
                final File[] children = dir.listFiles();
                if (children == null || children.length == 0) {
                    continue;
                }
                for (File child : children) {
                    if (child == null) {
                        continue;
                    }
                    final String name = child.getName();
                    if (name == null || name.isEmpty()) {
                        continue;
                    }
                    if (child.isDirectory()) {
                        stack.push(new DirFrame(child, prefix + name + "/"));
                        continue;
                    }
                    found.add(normalizePath(prefix + name));
                }
            }

            return found;
        }

        private static final class DirFrame {
            private final File dir;
            private final String prefix;

            private DirFrame(final File dir, final String prefix) {
                this.dir = dir;
                this.prefix = prefix;
            }
        }
    }

    private static String normalizeKey(final File directory) {
        String key = directory.getAbsolutePath();
        if (key.indexOf('\\') >= 0) {
            key = key.replace('\\', '/');
        }
        return CASE_INSENSITIVE ? key.toLowerCase(Locale.ROOT) : key;
    }

    private static String normalizePath(final String path) {
        String normalized = path;
        if (normalized.indexOf('\\') >= 0) {
            normalized = normalized.replace('\\', '/');
        }
        while (!normalized.isEmpty() && normalized.charAt(0) == '/') {
            normalized = normalized.substring(1);
        }
        return CASE_INSENSITIVE ? normalized.toLowerCase(Locale.ROOT) : normalized;
    }

    private static boolean isWindows() {
        final String os = System.getProperty("os.name");
        return os != null && os.toLowerCase(Locale.ROOT).contains("win");
    }
}
