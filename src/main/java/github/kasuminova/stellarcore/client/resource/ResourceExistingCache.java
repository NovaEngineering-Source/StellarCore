package github.kasuminova.stellarcore.client.resource;

import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.util.List;
import java.util.Set;

public class ResourceExistingCache {

    private static final Set<StellarCoreResourcePack> RESOURCE_PACKS = new ReferenceOpenHashSet<>();

    public static void addResourcePack(StellarCoreResourcePack resourcePack) {
        RESOURCE_PACKS.add(resourcePack);
        resourcePack.stellar_core$onReload();
    }

    public static void clear() {
        List<StellarCoreResourcePack> persistentResourcePacks = new ObjectArrayList<>();
        RESOURCE_PACKS.forEach(resourcePack -> {
            resourcePack.stellar_core$disableCache();
            if (resourcePack.stellar_core$isPersistent()) {
                persistentResourcePacks.add(resourcePack);
            }
        });
        RESOURCE_PACKS.clear();
        RESOURCE_PACKS.addAll(persistentResourcePacks);
        StellarLog.LOG.info("[StellarCore-ResourceExistingCache] Resource cache cleared.");
    }

    public static void enableCache() {
        RESOURCE_PACKS.forEach(StellarCoreResourcePack::stellar_core$enableCache);
        StellarLog.LOG.info("[StellarCore-ResourceExistingCache] Resource cache enabled.");
    }

    public static void disableCache() {
        RESOURCE_PACKS.forEach(StellarCoreResourcePack::stellar_core$disableCache);
        StellarLog.LOG.info("[StellarCore-ResourceExistingCache] Resource cache disabled.");
    }

}
