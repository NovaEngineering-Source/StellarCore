package github.kasuminova.stellarcore.client.resource;

import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.util.Set;

public class ResourceExistingCache {

    private static final Set<StellarCoreResourcePack> RESOURCE_PACKS = new ReferenceOpenHashSet<>();

    public static void addResourcePack(StellarCoreResourcePack resourcePack) {
        RESOURCE_PACKS.add(resourcePack);
        resourcePack.stellar_core$onReload();
    }

    public static void clear() {
        RESOURCE_PACKS.forEach(StellarCoreResourcePack::stellar_core$disableCache);
        RESOURCE_PACKS.clear();
    }

    public static void enableCache() {
        RESOURCE_PACKS.forEach(StellarCoreResourcePack::stellar_core$enableCache);
    }

    public static void disableCache() {
        RESOURCE_PACKS.forEach(StellarCoreResourcePack::stellar_core$disableCache);
    }

}
