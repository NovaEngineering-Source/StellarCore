package github.kasuminova.stellarcore.mixin.minecraft.forge;

import github.kasuminova.stellarcore.mixin.util.CustomLoadingScreenUtils;
import net.minecraftforge.fml.client.SplashProgress;
import net.minecraftforge.fml.common.Loader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(SplashProgress.class)
public class MixinSplashProgress {

    @Inject(method = "finish", at = @At("TAIL"), remap = false)
    private static void injectFinish(final CallbackInfo ci) {
        if (Loader.isModLoaded("customloadingscreen")) {
            CustomLoadingScreenUtils.cleanCLSTextures();
        }
    }

}
