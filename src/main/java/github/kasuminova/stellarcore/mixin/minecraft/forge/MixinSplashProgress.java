package github.kasuminova.stellarcore.mixin.minecraft.forge;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.CustomLoadingScreenUtils;
import github.kasuminova.stellarcore.mixin.util.ModLoaderEarly;
import net.minecraftforge.fml.client.SplashProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashProgress.class)
public class MixinSplashProgress {

    @Inject(method = "finish", at = @At("TAIL"), remap = false)
    private static void injectFinish(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.customLoadingScreen.splashProgress) {
            return;
        }
        if (ModLoaderEarly.isModLoad("customloadingscreen")) {
            CustomLoadingScreenUtils.cleanCLSTextures();
        }
    }

}
