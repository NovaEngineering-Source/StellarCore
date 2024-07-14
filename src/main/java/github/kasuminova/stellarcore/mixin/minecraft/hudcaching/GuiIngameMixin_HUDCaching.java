package github.kasuminova.stellarcore.mixin.minecraft.hudcaching;

import github.kasuminova.stellarcore.client.hudcaching.HUDCaching;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public class GuiIngameMixin_HUDCaching {
    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void patcher$cancelVignette(CallbackInfo ci) {
        if (HUDCaching.renderingCacheOverride) {
            ci.cancel();
        }
    }
}
