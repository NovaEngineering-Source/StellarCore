package github.kasuminova.stellarcore.common;

import github.kasuminova.stellarcore.common.integration.fluxnetworks.IntegrationsFluxNetworks;
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
    }

    public void init() {

    }

    public void postInit() {

    }

    public void loadComplete() {

    }
}
