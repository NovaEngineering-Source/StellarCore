package github.kasuminova.stellarcore.mixin.enderioconduits;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "crazypants.enderio.conduits.conduit.liquid.EnderLiquidConduitNetwork$NetworkTankKey", remap = false)
public class MixinEnderLiquidConduitNetworkNetworkTankKey {

    @Unique
    private int stellar_core$hashCodeCache;

    @Inject(method = "<init>(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)V", at = @At("RETURN"))
    private void injectInit(final BlockPos conduitLoc, final EnumFacing conDir, final CallbackInfo ci) {
        final int prime = 31;
        int hashCode = 1;
        hashCode = prime * hashCode + ((conDir == null) ? 0 : conDir.hashCode());
        hashCode = prime * hashCode + ((conduitLoc == null) ? 0 : conduitLoc.hashCode());
        stellar_core$hashCodeCache = hashCode;
    }

    @Inject(method = "hashCode", at = @At("HEAD"), cancellable = true)
    private void injectHashCode(final CallbackInfoReturnable<Integer> cir) {
        if (!StellarCoreConfig.PERFORMANCE.enderIOConduits.networkTankKeyHashCodeCache) {
            return;
        }
        cir.setReturnValue(stellar_core$hashCodeCache);
    }

}
