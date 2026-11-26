package github.kasuminova.stellarcore.common.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class GeckoZipResourcePaths {
    private GeckoZipResourcePaths() {
    }

    public static void enumerate(final ZipFile zip, final Set<String> domains, final String directory,
                                 final Predicate<String> predicate, final BiConsumer<String, String> found) {
        final Map<String, Prefix> prefixes = new Object2ObjectOpenHashMap<>();
        final Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            final String name = entries.nextElement().getName();
            // Predicates can change the live domain set; preserve entry-major iteration.
            for (final String domain : domains) {
                Prefix prefix = prefixes.get(domain);
                if (prefix == null) {
                    final String root = "assets/" + domain + '/';
                    prefix = new Prefix(root.length(), root + directory + '/');
                    prefixes.put(domain, prefix);
                }
                if (name.startsWith(prefix.path) && predicate.test(name)) {
                    found.accept(domain, name.substring(prefix.rootLength));
                }
            }
        }
    }

    private static final class Prefix {
        private final int rootLength;
        private final String path;

        private Prefix(final int rootLength, final String path) {
            this.rootLength = rootLength;
            this.path = path;
        }
    }
}
