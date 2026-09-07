package github.kasuminova.stellarcore.client.resource;

import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ResourceExistingCache {

    private static final NonBlockingHashSet<StellarCoreResourcePack> RESOURCE_PACKS = new NonBlockingHashSet<>();

    private static final Object LIFECYCLE_LOCK = new Object();

    private static boolean enabled = false;

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
            StellarLog.LOG.info("[StellarCore-ResourceExistingCache] Resource cache cleared.");
        }
    }

    public static void enableCache() {
        synchronized (LIFECYCLE_LOCK) {
            if (enabled) {
                return;
            }
            DirectoryPathIndex.clear();
            RESOURCE_PACKS.forEach(StellarCoreResourcePack::stellar_core$enableCache);
            enabled = true;
            StellarLog.LOG.info("[StellarCore-ResourceExistingCache] Resource cache enabled.");
        }
    }

    public static void disableCache() {
        synchronized (LIFECYCLE_LOCK) {
            RESOURCE_PACKS.forEach(StellarCoreResourcePack::stellar_core$disableCache);
            enabled = false;
            StellarLog.LOG.info("[StellarCore-ResourceExistingCache] Resource cache disabled.");
        }
    }

}
