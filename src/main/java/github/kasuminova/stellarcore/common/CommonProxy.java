package github.kasuminova.stellarcore.common;

import github.kasuminova.stellarcore.common.bugfix.TileEntityContainerFixes;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.entity.EntityForceUpdateManager;
import github.kasuminova.stellarcore.common.handler.StellarCoreTickHandler;
import github.kasuminova.stellarcore.common.integration.fluxnetworks.IntegrationsFluxNetworks;
import github.kasuminova.stellarcore.common.itemstack.ItemStackCapInitializer;
import github.kasuminova.stellarcore.common.pool.LowerCaseStringPool;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;

@SuppressWarnings("MethodMayBeStatic")
public class CommonProxy {

    public CommonProxy() {
    }

    public void construction() {

    }

    public void preInit() {
        if (Loader.isModLoaded("fluxnetworks")) {
            IntegrationsFluxNetworks.preInit();
        }
        MinecraftForge.EVENT_BUS.register(TileEntityContainerFixes.INSTANCE);
        MinecraftForge.EVENT_BUS.register(StellarCoreTickHandler.class);
        MinecraftForge.EVENT_BUS.register(EntityForceUpdateManager.INSTANCE);
    }

    public void init() {

    }

    public void postInit() {

    }

    public void loadComplete() {
        LowerCaseStringPool.INSTANCE.clear();
        if (StellarCoreConfig.PERFORMANCE.vanilla.asyncItemStackCapabilityInit) {
            ItemStackCapInitializer.resetStatus();
        }
    }

}
