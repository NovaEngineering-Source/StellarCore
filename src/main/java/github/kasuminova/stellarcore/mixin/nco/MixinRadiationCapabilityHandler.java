package github.kasuminova.stellarcore.mixin.nco;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import nc.capability.radiation.RadiationCapabilityHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 禁用 Nuclearcraft Overhauled 的辐射能力系统。
 */
@SuppressWarnings("MethodMayBeStatic")
@Mixin(RadiationCapabilityHandler.class)
public class MixinRadiationCapabilityHandler {
    @Inject(method = "attachEntityRadiationCapability", at = @At("HEAD"), cancellable = true, remap = false)
    public void onAttachEntityRadiationCapability(CallbackInfo ci) {
        if (!StellarCoreConfig.FEATURES.nuclearCraftOverhauled.removeRadiationCapabilityHandler) {
            return;
        }
        ci.cancel();
    }

    @Inject(method = "attachChunkRadiationCapability", at = @At("HEAD"), cancellable = true, remap = false)
    public void onAttachChunkRadiationCapability(CallbackInfo ci) {
        if (!StellarCoreConfig.FEATURES.nuclearCraftOverhauled.removeRadiationCapabilityHandler) {
            return;
        }
        ci.cancel();
    }

    @Inject(method = "attachTileRadiationCapability", at = @At("HEAD"), cancellable = true, remap = false)
    public void onAttachTileRadiationCapability(CallbackInfo ci) {
        if (!StellarCoreConfig.FEATURES.nuclearCraftOverhauled.removeRadiationCapabilityHandler) {
            return;
        }
        ci.cancel();
    }

    @Inject(method = "attachStackRadiationCapability", at = @At("HEAD"), cancellable = true, remap = false)
    public void onAttachStackRadiationCapability(CallbackInfo ci) {
        if (!StellarCoreConfig.FEATURES.nuclearCraftOverhauled.removeRadiationCapabilityHandler) {
            return;
        }
        ci.cancel();
    }
}
