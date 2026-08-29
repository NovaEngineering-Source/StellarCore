package github.kasuminova.stellarcore.mixin.mek_top;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.integration.fluxnetworks.FluxPlugAcceptor;
import github.kasuminova.stellarcore.common.mod.Mods;
import mekanism.api.Coord4D;
import mekanism.common.base.EnergyAcceptorWrapper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sonar.fluxnetworks.common.tileentity.TileFluxPlug;

@Mixin(EnergyAcceptorWrapper.class)
public class MixinEnergyAcceptorWrapper {

    @Unique
    private static final boolean s_loadFlux = Mods.FN.loaded();

    @Inject(method = "get", at = @At("HEAD"), cancellable = true, remap = false)
    private static void injectGet(final TileEntity tileEntity, final EnumFacing side, final CallbackInfoReturnable<EnergyAcceptorWrapper> cir) {
        if (!StellarCoreConfig.FEATURES.mekanism.fluxNetworksSupport || !s_loadFlux) {
            return;
        }
        //noinspection ConstantValue
        if (tileEntity instanceof TileFluxPlug plug && tileEntity.getWorld() != null) {
            FluxPlugAcceptor wrapper = new FluxPlugAcceptor(plug, side);
            wrapper.coord = Coord4D.get(tileEntity);
            cir.setReturnValue(wrapper);
        }
    }

}