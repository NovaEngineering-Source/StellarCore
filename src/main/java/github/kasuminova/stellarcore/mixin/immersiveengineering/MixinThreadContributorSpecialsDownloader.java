package github.kasuminova.stellarcore.mixin.immersiveengineering;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = ImmersiveEngineering.ThreadContributorSpecialsDownloader.class, remap = false)
public class MixinThreadContributorSpecialsDownloader {

    @Inject(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lblusunrize/immersiveengineering/common/util/IELogger;warn(Ljava/lang/Object;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void injectRunLogWarn(final CallbackInfo ci, @Local(name = "excepParse") final Exception excepParse) throws Exception {
        if (!StellarCoreConfig.BUG_FIXES.immersiveEngineering.contributorSpecialsDownloader) {
            return;
        }
        throw excepParse;
    }

}
