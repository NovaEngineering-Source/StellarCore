package github.kasuminova.stellarcore.mixin.techguns;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import techguns.damagesystem.TGDamageSource;

@Mixin(TGDamageSource.class)
public class MixinTGDamageSource {

    @Inject(method = "setBehaviourForVanilla", at = @At("HEAD"), remap = false)
    private void onSetBehaviourForVanilla(final CallbackInfo ci) {
        if (!StellarCoreConfig.FEATURES.techguns.tgDamageSource) {
            return;
        }
        ((TGDamageSource) ((Object) this)).setProjectile();
    }

}
