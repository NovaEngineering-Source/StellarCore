package github.kasuminova.stellarcore.client;

import github.kasuminova.stellarcore.client.handler.ClientEventHandler;
import github.kasuminova.stellarcore.client.integration.libnine.L9ModScanner;
import github.kasuminova.stellarcore.client.model.vanillacache.VanillaModelDiskCache;
import github.kasuminova.stellarcore.client.model.vanillacache.VanillaModelDiskCacheReloadListener;
import github.kasuminova.stellarcore.client.pool.BakedQuadPool;
import github.kasuminova.stellarcore.client.pool.BlockFaceUVsPool;
import github.kasuminova.stellarcore.client.pool.StellarUnpackedDataPool;
import github.kasuminova.stellarcore.client.resource.ClasspathAssetIndex;
import github.kasuminova.stellarcore.client.util.TitleUtils;
import github.kasuminova.stellarcore.common.CommonProxy;
import github.kasuminova.stellarcore.common.command.CommandStellarCoreClient;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.mod.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;

import java.util.Collections;
import java.util.Set;

public class ClientProxy extends CommonProxy {

    private static final Set<String> STELLAR_CORE$DEFAULT_RESOURCE_DOMAINS = Collections.singleton("minecraft");

    @Override
    public void construction() {
        super.construction();

        if (StellarCoreConfig.PERFORMANCE.vanilla.resourceExistStateCache) {
            ClasspathAssetIndex.prewarmAsync(STELLAR_CORE$DEFAULT_RESOURCE_DOMAINS);
        }

        // Start the whole prepare pipeline now: fingerprint the environment, then
        // read the cache file for it. Construction happens before preInit and
        // init, and the model loader does not run until the resource refresh that
        // follows init, so this has the entire mod-loading window to finish
        // without ever delaying a model.
        if (VanillaModelDiskCache.INSTANCE.isEnabled()) {
            VanillaModelDiskCache.INSTANCE.prepareAsync(Loader.instance().getConfigDir());
        }

        TitleUtils.setRandomTitle("*Construction*");
    }

    @Override
    public void preInit() {
        super.preInit();
        MinecraftForge.EVENT_BUS.register(ClientEventHandler.INSTANCE);

        if (StellarCoreConfig.PERFORMANCE.vanilla.resourceExistStateCache) {
            ClasspathAssetIndex.prewarmAsync(STELLAR_CORE$DEFAULT_RESOURCE_DOMAINS);
        }

        if (Mods.LIB_NINE.loaded()) {
            L9ModScanner.scan();
        }

        // Hook the vanilla-model disk cache into the resource reload pipeline so a
        // pack change invalidates it. The cache itself was already prepared back
        // in construction(); nothing to load here.
        if (VanillaModelDiskCache.INSTANCE.isEnabled()) {
            try {
                ((IReloadableResourceManager)
                        Minecraft.getMinecraft().getResourceManager())
                        .registerReloadListener(VanillaModelDiskCacheReloadListener.INSTANCE);
            } catch (Throwable ignored) {
                // The disk cache must never crash the loading pipeline.
            }
        }

//        if (Mods.REPLAY.loaded() && StellarCoreConfig.PERFORMANCE.vanilla.hudCaching) {
//            StellarCoreConfig.PERFORMANCE.vanilla.hudCaching = false;
//            StellarLog.LOG.warn("Replay Mod is not compatible with Performance/HUDCaching feature, auto disabled!");
//        }

        TitleUtils.setRandomTitle("*PreInit*");
    }

    @Override
    public void init() {
        super.init();

        TitleUtils.setRandomTitle("*Init*");
    }

    @Override
    public void postInit() {
        super.postInit();

        ClientCommandHandler.instance.registerCommand(CommandStellarCoreClient.INSTANCE);

        TitleUtils.setRandomTitle("*PostInit*");
    }

    @Override
    public void loadComplete() {
        super.loadComplete();

        TitleUtils.setRandomTitle();
        StellarUnpackedDataPool.reset();
        BakedQuadPool.INSTANCE.clear();
        BlockFaceUVsPool.INSTANCE.clear();

        // Persist any vanilla JSON model snapshots captured during this run in the
        // background. Runs after the entire model pipeline finished, so we aren't
        // racing with the bake worker threads and don't block the loading screen.
        final VanillaModelDiskCache diskCache = VanillaModelDiskCache.INSTANCE;
        if (diskCache.isEnabled()) {
            try {
                diskCache.saveAsync(Loader.instance().getConfigDir());
            } catch (Throwable ignored) {
                // Disk cache must never crash the loading pipeline.
            }
        }
    }

}
