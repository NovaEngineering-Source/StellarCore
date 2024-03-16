package github.kasuminova.stellarcore.mixin.mek_top;

import github.kasuminova.stellarcore.common.integration.fluxnetworks.FluxPlugAcceptor;
import mekanism.api.Coord4D;
import mekanism.common.base.EnergyAcceptorWrapper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sonar.fluxnetworks.common.tileentity.TileFluxPlug;

@Mixin(EnergyAcceptorWrapper.class)
public class MixinEnergyAcceptorWrapper {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true, remap = false)
    private static void injectGet(final TileEntity te, final EnumFacing side, final CallbackInfoReturnable<EnergyAcceptorWrapper> cir) {
        //noinspection ConstantValue
        if (te instanceof TileFluxPlug plug && te.getWorld() != null) {
            FluxPlugAcceptor wrapper = new FluxPlugAcceptor(plug, side);
            wrapper.coord = Coord4D.get(te);
            cir.setReturnValue(wrapper);
        }
    }

}
