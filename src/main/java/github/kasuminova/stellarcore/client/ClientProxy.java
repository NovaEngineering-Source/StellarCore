package github.kasuminova.stellarcore.client;

import github.kasuminova.stellarcore.client.handler.ClientEventHandler;
import github.kasuminova.stellarcore.client.integration.libnine.L9ModScanner;
import github.kasuminova.stellarcore.client.pool.BakedQuadPool;
import github.kasuminova.stellarcore.client.pool.BlockFaceUVsPool;
import github.kasuminova.stellarcore.client.pool.StellarUnpackedDataPool;
import github.kasuminova.stellarcore.client.resource.ClasspathAssetIndex;
import github.kasuminova.stellarcore.client.util.TitleUtils;
import github.kasuminova.stellarcore.common.CommonProxy;
import github.kasuminova.stellarcore.common.command.CommandStellarCoreClient;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.mod.Mods;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {

    private static final java.util.Set<String> STELLAR_CORE$DEFAULT_RESOURCE_DOMAINS = java.util.Collections.singleton("minecraft");

    @Override
    public void construction() {
        super.construction();

        if (StellarCoreConfig.PERFORMANCE.vanilla.resourceExistStateCache) {
            // Kick off classpath index building as early as possible.
            // Only prewarm the hot namespace by default to avoid scanning the entire classpath twice.
            ClasspathAssetIndex.prewarmAsync(STELLAR_CORE$DEFAULT_RESOURCE_DOMAINS);
        }

        TitleUtils.setRandomTitle("*Construction*");
    }

    @Override
    public void preInit() {
        super.preInit();
        MinecraftForge.EVENT_BUS.register(ClientEventHandler.INSTANCE);

        if (StellarCoreConfig.PERFORMANCE.vanilla.resourceExistStateCache) {
            // Kick off classpath index building early to avoid stalling the main thread during resource reload.
            ClasspathAssetIndex.prewarmAsync(STELLAR_CORE$DEFAULT_RESOURCE_DOMAINS);
        }

        if (Mods.LIB_NINE.loaded()) {
            L9ModScanner.scan();
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
    }

}
