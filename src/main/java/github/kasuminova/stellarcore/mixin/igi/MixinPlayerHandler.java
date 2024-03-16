package github.kasuminova.stellarcore.mixin.igi;

import com.github.lunatrius.ingameinfo.handler.PlayerHandler;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerHandler.class)
public class MixinPlayerHandler {

    @SuppressWarnings("MethodMayBeStatic")
    @Inject(method = "onPlayerLogin", at = @At("HEAD"), cancellable = true, remap = false)
    private void prevOnPlayerLogin(final CallbackInfo ci) {
        if (!StellarCoreConfig.BUG_FIXES.inGameInfoXML.playerHandler) {
            return;
        }
        ci.cancel();
    }

}