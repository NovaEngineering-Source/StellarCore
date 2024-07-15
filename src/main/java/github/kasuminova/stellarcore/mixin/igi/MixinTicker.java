package github.kasuminova.stellarcore.mixin.igi;

import com.github.lunatrius.ingameinfo.handler.Ticker;
import github.kasuminova.stellarcore.client.hudcaching.HUDCaching;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(Ticker.class)
public class MixinTicker {

    @Inject(method = "onRenderTick", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectOnRenderTick(final TickEvent.RenderTickEvent event, final CallbackInfo ci) {
        if (StellarCoreConfig.PERFORMANCE.inGameInfoXML.hudFrameBuffer && StellarCoreConfig.PERFORMANCE.vanilla.hudCaching && !HUDCaching.igiRendering) {
            ci.cancel();
        }
    }

}
