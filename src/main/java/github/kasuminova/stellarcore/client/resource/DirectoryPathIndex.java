package github.kasuminova.stellarcore.client.resource;

import com.github.bsideup.jabel.Desugar;
import github.kasuminova.stellarcore.common.util.StellarEnvironment;
import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class DirectoryPathIndex {

    private static final boolean CASE_INSENSITIVE = isWindows();
    private static final NonBlockingHashMap<String, Index> INDEXES = new NonBlockingHashMap<>();
    private static final AtomicLong GENERATION = new AtomicLong();
    private static final int MAX_SCAN_THREADS = 4;

    private static volatile boolean negativeCachingEnabled;

    private static volatile ExecutorService executor;

    private DirectoryPathIndex() {
    }

    public static void clear() {
        GENERATION.incrementAndGet();
        INDEXES.clear();
        negativeCachingEnabled = false;
    }

    public static void enableNegativeCaching() {
        negativeCachingEnabled = true;
    }

    public static void disableNegativeCaching() {
        negativeCachingEnabled = false;
    }

    public static void prewarmAsync(@Nullable final File rootDirectory) {
        if (rootDirectory != null && rootDirectory.isDirectory()) {
            currentIndex(rootDirectory).ensureInitializedAsync();
        }
    }

    public static boolean contains(@Nullable final File rootDirectory, @Nullable final String relativePath) {
        return contains(rootDirectory, relativePath, null);
    }

    public static boolean contains(@Nullable final File rootDirectory,
                                   @Nullable final String relativePath,
                                   @Nullable final File candidateFile) {
        if (rootDirectory == null || !isSafeRelativePath(relativePath)) {
            return false;
        }

        final String normalizedPath = normalizePath(relativePath);
        while (true) {
            final Index index = currentIndex(rootDirectory);
            if (!index.isRootDirectory()) {
                return candidate(rootDirectory, relativePath, candidateFile).isFile();
            }
            if (index.contains(normalizedPath)) {
                if (index.isCurrent()) {
                    return true;
                }
                continue;
            }
            if (!index.isCurrent()) {
                continue;
            }

            index.ensureInitializedAsync();
            if (index.canUseNegativeResult()) {
                return false;
            }
            if (!candidate(rootDirectory, relativePath, candidateFile).isFile()) {
                return false;
            }
            if (index.addIfCurrent(normalizedPath)) {
                return true;
            }
        }
    }

    private static File candidate(final File rootDirectory, final String relativePath, @Nullable final File candidateFile) {
        return candidateFile != null ? candidateFile : new File(rootDirectory, relativePath);
    }

    private static boolean isSafeRelativePath(@Nullable final String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        final char first = path.charAt(0);
        if (first == '/' || first == '\\') {
            return false;
        }
        if (path.length() >= 2 && path.charAt(1) == ':' && Character.isLetter(path.charAt(0))) {
            return false;
        }
        if (path.indexOf('\\') >= 0) {
            return false;
        }
        if (!path.contains("..")) {
            return true;
        }
        return !path.equals("..")
            && !path.startsWith("../")
            && !path.endsWith("/..")
            && !path.contains("/../");
    }

    private static Index currentIndex(final File rootDirectory) {
        final String key = normalizeKey(rootDirectory);
        while (true) {
            final long generation = GENERATION.get();
            final Index existing = INDEXES.get(key);
            if (existing != null && existing.generation == generation) {
                return existing;
            }
            final Index created = new Index(rootDirectory, generation);
            final Index previous = INDEXES.putIfAbsent(key, created);
            final Index index = previous == null ? created : previous;
            if (index.isCurrent()) {
                return index;
            }
            INDEXES.remove(key, index);
        }
    }

    private static ExecutorService executor() {
        ExecutorService current = executor;
        if (current != null) {
            return current;
        }
        synchronized (DirectoryPathIndex.class) {
            current = executor;
            if (current != null) {
                return current;
            }
            final int threads = Math.max(1, Math.min(MAX_SCAN_THREADS, StellarEnvironment.getConcurrency()));
            final AtomicInteger threadId = new AtomicInteger();
            final ThreadFactory threadFactory = runnable -> {
                final Thread thread = new Thread(runnable);
                thread.setName("StellarCore-DirectoryPathIndex-" + threadId.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            };
            current = Executors.newFixedThreadPool(threads, threadFactory);
            executor = current;
            return current;
        }
    }

    private static String normalizeKey(final File directory) {
        return normalize(directory.getAbsolutePath());
    }

    private static String normalizePath(final String path) {
        return normalize(path);
    }

    private static String normalize(final String value) {
        final String normalized = value.indexOf('\\') >= 0 ? value.replace('\\', '/') : value;
        return CASE_INSENSITIVE ? normalized.toLowerCase(Locale.ROOT) : normalized;
    }

    private static boolean isWindows() {
        final String osName = System.getProperty("os.name", "");
        return osName.toLowerCase(Locale.ROOT).contains("win");
    }

    private static final class Index {
        private final File root;
        private final long generation;
        private final boolean rootDirectory;
        private final NonBlockingHashSet<String> paths = new NonBlockingHashSet<>();

        private volatile boolean initializationStarted;
        private volatile boolean initialized;
        private volatile boolean initializationFailed;

        private Index(final File root, final long generation) {
            this.root = root;
            this.generation = generation;
            
            
            this.rootDirectory = root.isDirectory();
        }

        private boolean isRootDirectory() {
            return this.rootDirectory;
        }

        private boolean contains(final String path) {
            return paths.contains(path);
        }

        private boolean canUseNegativeResult() {
            return initialized && !initializationFailed && negativeCachingEnabled && isCurrent();
        }

        private boolean addIfCurrent(final String path) {
            if (!isCurrent()) {
                return false;
            }
            paths.add(path);
            return isCurrent();
        }

        private boolean isCurrent() {
            return generation == GENERATION.get();
        }

        private void ensureInitializedAsync() {
            if (initializationStarted || !isCurrent()) {
                return;
            }
            synchronized (this) {
                if (initializationStarted || !isCurrent()) {
                    return;
                }
                initializationStarted = true;
                CompletableFuture.runAsync(this::initialize, DirectoryPathIndex.executor());
            }
        }

        private void initialize() {
            try {
                scan();
                if (isCurrent()) {
                    initialized = true;
                }
            } catch (Throwable throwable) {
                initializationFailed = true;
                StellarLog.LOG.error(
                    "[StellarCore-DirectoryPathIndex] Failed to scan directory index. root={}",
                    root.getAbsolutePath(), throwable
                );
                if (throwable instanceof Error) {
                    throw (Error) throwable;
                }
                throw new CompletionException(throwable);
            }
        }

        private void scan() throws IOException {
            if (!isCurrent() || !root.exists()) {
                return;
            }
            if (!root.isDirectory()) {
                throw new IOException("Directory index root is not a directory: " + root.getAbsolutePath());
            }

            final ObjectArrayList<DirectoryFrame> directories = new ObjectArrayList<>();
            directories.push(new DirectoryFrame(root, ""));
            while (!directories.isEmpty()) {
                if (!isCurrent()) {
                    return;
                }
                final DirectoryFrame frame = directories.pop();
                final File[] children = frame.directory.listFiles();
                if (children == null) {
                    throw new IOException("Unable to list directory: " + frame.directory.getAbsolutePath());
                }
                for (File child : children) {
                    if (!isCurrent()) {
                        return;
                    }
                    if (child.isDirectory()) {
                        directories.push(new DirectoryFrame(child, frame.prefix + child.getName() + "/"));
                    } else if (child.isFile()) {
                        paths.add(normalizePath(frame.prefix + child.getName()));
                    }
                }
            }
        }
    }

    @Desugar
    private record DirectoryFrame(File directory, String prefix) {
    }
}
