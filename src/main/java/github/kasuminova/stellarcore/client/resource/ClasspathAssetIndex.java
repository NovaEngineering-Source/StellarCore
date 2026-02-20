package github.kasuminova.stellarcore.client.resource;

import github.kasuminova.stellarcore.common.util.StellarEnvironment;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * A minimal "virtual filesystem" for classpath assets.
 *
 * <p>DefaultResourcePack falls back to {@link Class#getResource} / {@link Class#getResourceAsStream}
 * which can become expensive when the classpath contains many jars and most lookups are misses.
 * This index scans classpath entries once and allows O(1) existence queries for assets.
 */
public final class ClasspathAssetIndex {

    private static final ConcurrentHashMap<String, NamespaceIndex> NAMESPACE_INDEX = new ConcurrentHashMap<>();

    private static final int MAX_SCAN_THREADS = 8;
    private static volatile int executorThreads = 0;
    private static volatile ExecutorService executor;

    private static volatile ClasspathSources cachedSources;

    private ClasspathAssetIndex() {
    }

    private static Executor executor() {
        ExecutorService current = executor;
        if (current != null) {
            return current;
        }
        synchronized (ClasspathAssetIndex.class) {
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
                thread.setName("StellarCore-ClasspathAssetIndex-" + threadId.getAndIncrement());
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

    private static ClasspathSources getClasspathSources() {
        ClasspathSources sources = cachedSources;
        if (sources != null) {
            return sources;
        }
        synchronized (ClasspathAssetIndex.class) {
            sources = cachedSources;
            if (sources != null) {
                return sources;
            }
            sources = computeClasspathSources();
            cachedSources = sources;
            return sources;
        }
    }

    private static ClasspathSources computeClasspathSources() {
        final LinkedHashSet<File> files = new LinkedHashSet<>();

        final ClassLoader cl = DefaultResourcePack.class.getClassLoader();
        if (cl instanceof URLClassLoader) {
            final URL[] urls = ((URLClassLoader) cl).getURLs();
            if (urls != null) {
                for (URL url : urls) {
                    File file = toFile(url);
                    if (file != null) {
                        files.add(file);
                    }
                }
            }
        }

        // Fallback: may be incomplete if the runtime classpath is mutated at runtime.
        if (files.isEmpty()) {
            final String cp = System.getProperty("java.class.path");
            if (cp != null && !cp.isEmpty()) {
                final String[] parts = cp.split(Pattern.quote(File.pathSeparator));
                for (String part : parts) {
                    if (part == null || part.isEmpty()) {
                        continue;
                    }
                    files.add(new File(part));
                }
            }
        }

        final List<File> jarFiles = new ArrayList<>();
        final List<File> directoryRoots = new ArrayList<>();
        for (File file : files) {
            if (file == null) {
                continue;
            }
            final String name = file.getName();
            if (name != null && name.endsWith(".jar")) {
                jarFiles.add(file);
                continue;
            }
            if (file.isDirectory()) {
                directoryRoots.add(file);
            }
        }

        return new ClasspathSources(
                Collections.unmodifiableList(jarFiles),
                Collections.unmodifiableList(directoryRoots)
        );
    }

    public static void prewarm(final Iterable<String> namespaces) {
        if (namespaces == null) {
            return;
        }
        for (String namespace : namespaces) {
            if (namespace == null || namespace.isEmpty()) {
                continue;
            }
            NAMESPACE_INDEX.computeIfAbsent(namespace, NamespaceIndex::new).ensureInitialized();
        }
    }

    public static void prewarmAsync(final Iterable<String> namespaces) {
        if (namespaces == null) {
            return;
        }
        for (String namespace : namespaces) {
            if (namespace == null || namespace.isEmpty()) {
                continue;
            }
            NAMESPACE_INDEX.computeIfAbsent(namespace, NamespaceIndex::new).ensureInitializedAsync();
        }
    }

    /**
     * Non-blocking existence check.
     *
     * @return {@code Boolean.TRUE}/{@code Boolean.FALSE} if the namespace index is ready;
     * {@code null} if the index is not initialized yet.
     */
    @Nullable
    public static Boolean tryContains(final ResourceLocation location) {
        if (location == null) {
            return Boolean.FALSE;
        }
        final String namespace = location.getNamespace();
        if (namespace == null || namespace.isEmpty()) {
            return Boolean.FALSE;
        }
        final NamespaceIndex index = NAMESPACE_INDEX.get(namespace);
        if (index == null) {
            return null;
        }
        return index.tryContains(location.getPath());
    }

    public static boolean contains(final ResourceLocation location) {
        if (location == null) {
            return false;
        }
        String namespace = location.getNamespace();
        if (namespace == null || namespace.isEmpty()) {
            return false;
        }
        return NAMESPACE_INDEX.computeIfAbsent(namespace, NamespaceIndex::new).contains(location.getPath());
    }

    private static final class NamespaceIndex {
        private final String namespace;

        private volatile boolean initialized = false;
        private volatile CompletableFuture<Void> initFuture;
        private volatile Set<String> jarPaths = Collections.emptySet();
        private volatile List<File> directoryRoots = Collections.emptyList();

        private NamespaceIndex(final String namespace) {
            this.namespace = namespace;
        }

        private boolean contains(final String path) {
            if (path == null || path.isEmpty()) {
                return false;
            }
            ensureInitialized();
            final String normalized = normalizeQueryPath(path);
            if (normalized.isEmpty()) {
                return false;
            }
            if (jarPaths.contains(normalized)) {
                return true;
            }
            if (!directoryRoots.isEmpty()) {
                final String normalizedPath = normalized.indexOf('/') >= 0 ? normalized.replace('/', File.separatorChar) : normalized;
                for (File namespaceRoot : directoryRoots) {
                    File candidate = new File(namespaceRoot, normalizedPath);
                    if (candidate.isFile()) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Nullable
        private Boolean tryContains(final String path) {
            if (path == null || path.isEmpty()) {
                return Boolean.FALSE;
            }
            if (!initialized) {
                ensureInitializedAsync();
                return null;
            }
            final String normalized = normalizeQueryPath(path);
            if (normalized.isEmpty()) {
                return Boolean.FALSE;
            }
            if (jarPaths.contains(normalized)) {
                return Boolean.TRUE;
            }
            if (!directoryRoots.isEmpty()) {
                final String normalizedPath = normalized.indexOf('/') >= 0 ? normalized.replace('/', File.separatorChar) : normalized;
                for (File namespaceRoot : directoryRoots) {
                    File candidate = new File(namespaceRoot, normalizedPath);
                    if (candidate.isFile()) {
                        return Boolean.TRUE;
                    }
                }
            }
            return Boolean.FALSE;
        }

        private String normalizeQueryPath(final String path) {
            String normalized = path;
            if (normalized.indexOf('\\') >= 0) {
                normalized = normalized.replace('\\', '/');
            }
            while (!normalized.isEmpty() && normalized.charAt(0) == '/') {
                normalized = normalized.substring(1);
            }

            final String assetsPrefix = "assets/" + namespace + "/";
            if (normalized.startsWith(assetsPrefix)) {
                normalized = normalized.substring(assetsPrefix.length());
            }

            return normalized;
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
                        this.jarPaths = Collections.emptySet();
                        this.directoryRoots = Collections.emptyList();
                    } finally {
                        initialized = true;
                    }
                }, ClasspathAssetIndex.executor());
            }
        }

        private void ensureInitialized() {
            if (initialized) {
                return;
            }
            final CompletableFuture<Void> current = initFuture;
            if (current != null) {
                try {
                    current.join();
                } catch (Throwable ignored) {
                }
                return;
            }
            synchronized (this) {
                if (initialized) {
                    return;
                }
                init();
                initialized = true;
            }
        }

        private void init() {
            final ClasspathSources sources = ClasspathAssetIndex.getClasspathSources();
            if (sources.jarFiles.isEmpty() && sources.directoryRoots.isEmpty()) {
                this.jarPaths = Collections.emptySet();
                this.directoryRoots = Collections.emptyList();
                return;
            }

            final String prefix = "assets/" + namespace + "/";
            final Set<String> found = scanJars(prefix, sources.jarFiles);
            this.jarPaths = Collections.unmodifiableSet(found);

            final List<File> namespaceRoots = new ArrayList<>();
            if (!sources.directoryRoots.isEmpty()) {
                final String base = "assets" + File.separatorChar + namespace;
                for (File root : sources.directoryRoots) {
                    File namespaceRoot = new File(root, base);
                    if (namespaceRoot.isDirectory()) {
                        namespaceRoots.add(namespaceRoot);
                    }
                }
            }
            this.directoryRoots = Collections.unmodifiableList(namespaceRoots);
        }

        private Set<String> scanJars(final String prefix, final List<File> jarFiles) {
            if (jarFiles == null || jarFiles.isEmpty()) {
                return new HashSet<>();
            }

            final int threads = ClasspathAssetIndex.executorThreads();
            final int taskCount = threads <= 1 ? 1 : Math.min(threads, jarFiles.size());
            if (taskCount <= 1) {
                final Set<String> found = new HashSet<>();
                for (File jarFile : jarFiles) {
                    scanJar(jarFile, found, prefix);
                }
                return found;
            }

            final Executor exec = ClasspathAssetIndex.executor();
            final List<CompletableFuture<Set<String>>> tasks = new ArrayList<>(taskCount);
            for (int taskId = 0; taskId < taskCount; taskId++) {
                final int start = taskId;
                tasks.add(CompletableFuture.supplyAsync(() -> {
                    final Set<String> local = new HashSet<>();
                    for (int i = start; i < jarFiles.size(); i += taskCount) {
                        scanJar(jarFiles.get(i), local, prefix);
                    }
                    return local;
                }, exec));
            }

            final Set<String> found = new HashSet<>();
            for (CompletableFuture<Set<String>> task : tasks) {
                try {
                    found.addAll(task.join());
                } catch (Throwable ignored) {
                }
            }
            return found;
        }

        private void scanJar(final File jarFile, final Set<String> found, final String prefix) {
            if (jarFile == null) {
                return;
            }
            try (JarFile jar = new JarFile(jarFile, false)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry == null || entry.isDirectory()) {
                        continue;
                    }
                    String name = entry.getName();
                    if (name == null || !name.startsWith(prefix)) {
                        continue;
                    }
                    String relative = name.substring(prefix.length());
                    if (!relative.isEmpty()) {
                        found.add(relative);
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static final class ClasspathSources {
        private final List<File> jarFiles;
        private final List<File> directoryRoots;

        private ClasspathSources(final List<File> jarFiles, final List<File> directoryRoots) {
            this.jarFiles = jarFiles;
            this.directoryRoots = directoryRoots;
        }
    }

    @Nullable
    private static File toFile(@Nullable final URL url) {
        if (url == null) {
            return null;
        }
        if (!"file".equalsIgnoreCase(url.getProtocol())) {
            return null;
        }
        try {
            final URI uri = url.toURI();
            return new File(uri);
        } catch (URISyntaxException e) {
            final String path = url.getPath();
            return path == null ? null : new File(path);
        }
    }

}
