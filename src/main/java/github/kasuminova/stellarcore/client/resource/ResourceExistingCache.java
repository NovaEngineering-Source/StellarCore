package github.kasuminova.stellarcore.client.resource;

import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ResourceExistingCache {

    private static final NonBlockingHashSet<StellarCoreResourcePack> RESOURCE_PACKS = new NonBlockingHashSet<>();

    private static final Object LIFECYCLE_LOCK = new Object();

    private static boolean enabled = false;

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
            applyStateLocked(depth > 0, "Resource cache cleared");
        }
    }

    public static void enableCache() {
        synchronized (LIFECYCLE_LOCK) {
            depth++;
            if (!enabled) {
                applyStateLocked(true, "Resource cache enabled");
            }
        }
    }

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
