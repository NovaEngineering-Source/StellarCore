package github.kasuminova.stellarcore.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.metal.TileEntityMultiblockMetal;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = TileEntityMultiblockMetal.class, remap = false)
public class MixinTileEntityMultiblockMetal {

    @Inject(method = "postEnergyTransferUpdate", at = @At("HEAD"), cancellable = true)
    private void injectPostEnergyTransferUpdate(final int energy, final boolean simulate, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.immersiveEngineering.energyTransferNoUpdate) {
            return;
        }
        ci.cancel();
    }

}
