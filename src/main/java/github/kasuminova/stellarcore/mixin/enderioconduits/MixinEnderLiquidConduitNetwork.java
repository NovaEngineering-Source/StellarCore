package github.kasuminova.stellarcore.mixin.enderioconduits;

import crazypants.enderio.base.Log;
import crazypants.enderio.conduits.conduit.liquid.EnderLiquidConduitNetwork;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = EnderLiquidConduitNetwork.class, remap = false)
public class MixinEnderLiquidConduitNetwork {

    @Redirect(
            method = "tryTransfer",
            at = @At(
                    value = "INVOKE",
                    target = "Lcrazypants/enderio/base/Log;warn([Ljava/lang/Object;)V",
                    ordinal = 1
            )
    )
    private void redirectTryTransferWarn(final Object[] msg) {
        if (!StellarCoreConfig.FEATURES.enderIOConduits.prevEnderLiquidConduitLogSpam) {
            Log.warn(msg);
        }
        // Prev log spam.
    }

}
