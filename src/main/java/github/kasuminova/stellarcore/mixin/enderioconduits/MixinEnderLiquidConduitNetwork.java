package github.kasuminova.stellarcore.mixin.enderioconduits;

import crazypants.enderio.base.Log;
import crazypants.enderio.conduits.conduit.liquid.EnderLiquidConduitNetwork;
import crazypants.enderio.conduits.conduit.liquid.NetworkTank;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = EnderLiquidConduitNetwork.class, remap = false)
public class MixinEnderLiquidConduitNetwork {

    @Shadow
    Map<Object, NetworkTank> tankMap;

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

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.enderIOConduits.enderLiquidConduitNetworkTankMap) {
            return;
        }
        this.tankMap = new Object2ObjectOpenHashMap<>();
    }

}
