package github.kasuminova.stellarcore.mixin.tconevo;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(targets = "xyz.phanta.tconevo.integration.avaritia.client.AvaritiaMaterialModel$BakedAvaritiaMaterialModel$WithoutHalo")
public class MixinAvaritiaMaterialModel {

    @Inject(method = "handleCosmicLighting", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectHandleCosmicLighting(final ItemCameraTransforms.TransformType transType, final CallbackInfo ci) {
        if (!StellarCoreConfig.BUG_FIXES.tConEvo.handleCosmicLightingNPEFixes) {
            return;
        }
        if (transType == null) {
            ci.cancel();
        }
    }

}
