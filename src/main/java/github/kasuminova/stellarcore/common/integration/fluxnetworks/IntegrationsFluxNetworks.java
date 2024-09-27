package github.kasuminova.stellarcore.common.integration.fluxnetworks;

import github.kasuminova.stellarcore.common.mod.Mods;
import github.kasuminova.stellarcore.common.util.StellarLog;
import net.minecraftforge.fml.common.Optional;
import sonar.fluxnetworks.common.handler.TileEntityHandler;

public class IntegrationsFluxNetworks {

    @Optional.Method(modid = "fluxnetworks")
    public static void preInit() {
        if (Mods.MEK.loaded() && !Mods.MEKCEU.loaded()) {
            initMekanismIntegration();
        }
    }

    @Optional.Method(modid = "mekanism")
    public static void initMekanismIntegration() {
        //在列表头部插入适配器，保证不被其他类型覆盖结果。
        //Insert adapters in the head of the list to ensure that the results are not overwritten by other types.
        TileEntityHandler.tileEnergyHandlers.add(0, MekanismEnergyHandler.INSTANCE);
        StellarLog.LOG.info("Mekanism <===> FluxNetworks is initialized!");
    }

}
