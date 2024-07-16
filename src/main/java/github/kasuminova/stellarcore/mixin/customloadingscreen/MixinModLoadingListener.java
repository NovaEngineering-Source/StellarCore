package github.kasuminova.stellarcore.mixin.customloadingscreen;

import alexiil.mc.mod.load.ModLoadingListener;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModLoadingListener.class)
public class MixinModLoadingListener {

    @Redirect(
            method = "doProgress",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Thread;sleep(J)V",
                    remap = false
            ),
            remap = false
    )
    private static void redirectDoProgressSleep(final long l) {
        if (StellarCoreConfig.PERFORMANCE.customLoadingScreen.modLoadingListener) {
            // For our better future, don't do that.
            return;
        }
        try {
            // Congratulations on wasting 20ms of your life.
            Thread.sleep(l);
        } catch (InterruptedException e) {
        }
    }

}
