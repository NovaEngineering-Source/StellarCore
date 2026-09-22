package github.kasuminova.stellarcore.client.resource;

import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ResourceExistingCache {

    private static final NonBlockingHashSet<StellarCoreResourcePack> RESOURCE_PACKS = new NonBlockingHashSet<>();

    private static final Object LIFECYCLE_LOCK = new Object();

    private static boolean enabled = false;

    /**
     * How many cache windows are currently open.
     *
     * <p>A resource reload opens one for its whole duration, and the model loader opens a shorter one inside it.
     * Counting them keeps the inner window from closing the outer one, and keeps a clear performed during the
     * reload from leaving the rest of it without a cache.</p>
     */
    private static int depth = 0;

    public static void addResourcePack(StellarCoreResourcePack resourcePack) {
        resourcePack.stellar_core$onReload();
        synchronized (LIFECYCLE_LOCK) {
            RESOURCE_PACKS.add(resourcePack);
            if (enabled) {
                resourcePack.stellar_core$enableCache();
            }
        }
    }

    public static void clear() {
        synchronized (LIFECYCLE_LOCK) {
            final ObjectArrayList<StellarCoreResourcePack> persistentResourcePacks = new ObjectArrayList<>();
            RESOURCE_PACKS.forEach(resourcePack -> {
                resourcePack.stellar_core$disableCache();
                if (resourcePack.stellar_core$isPersistent()) {
                    persistentResourcePacks.add(resourcePack);
                }
            });
            RESOURCE_PACKS.clear();
            RESOURCE_PACKS.addAll(persistentResourcePacks);
            enabled = false;
            // A cleared index still has to answer whatever window is open around this reload.
            applyStateLocked(depth > 0, "Resource cache cleared");
        }
    }

    /** Opens a cache window; the cache stays on until the last one closes. */
    public static void enableCache() {
        synchronized (LIFECYCLE_LOCK) {
            depth++;
            if (!enabled) {
                applyStateLocked(true, "Resource cache enabled");
            }
        }
    }

    /** Closes the innermost cache window. */
    public static void disableCache() {
        synchronized (LIFECYCLE_LOCK) {
            if (depth > 0) {
                depth--;
            }
            if (depth == 0 && enabled) {
                applyStateLocked(false, "Resource cache disabled");
            }
        }
    }

    /**
     * Applies the requested state to the packs and to the directory index.
     *
     * @param target whether the cache should be on
     * @param reason log message describing the transition
     */
    private static void applyStateLocked(final boolean target, final String reason) {
        if (target) {
            DirectoryPathIndex.clear();
            RESOURCE_PACKS.forEach(StellarCoreResourcePack::stellar_core$enableCache);
            DirectoryPathIndex.enableNegativeCaching();
        } else {
            RESOURCE_PACKS.forEach(StellarCoreResourcePack::stellar_core$disableCache);
            DirectoryPathIndex.disableNegativeCaching();
        }
        enabled = target;
        StellarLog.LOG.info("[StellarCore-ResourceExistingCache] {}.", reason);
    }

}
