package github.kasuminova.stellarcore.client.integration.ctm;

import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import team.chisel.ctm.client.util.ResourceUtil;

/**
 * The parsed metadata and lookup dictionaries are needed while baking, not by baked quads.
 */
public final class CtmLoadCacheLifecycle implements StellarCoreResourcePack {
    public static final CtmLoadCacheLifecycle INSTANCE = new CtmLoadCacheLifecycle();

    private CtmLoadCacheLifecycle() {
    }

    @Override
    public void stellar_core$onReload() {
        ResourceUtil.invalidateCaches();
    }

    @Override
    public void stellar_core$disableCache() {
        ResourceUtil.invalidateCaches();
    }

    @Override
    public void stellar_core$enableCache() {
        ResourceUtil.invalidateCaches();
    }

    @Override
    public boolean stellar_core$isPersistent() {
        return true;
    }
}
