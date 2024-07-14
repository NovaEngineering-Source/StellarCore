package github.kasuminova.stellarcore.mixin.minecraft.hudcaching;

import github.kasuminova.stellarcore.client.hudcaching.HUDCaching;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameForge.class)
public class GuiIngameForgeMixin_HUDCaching {
    @Inject(method = "renderCrosshairs", at = @At("HEAD"), cancellable = true, remap = false)
    private void patcher$cancelCrosshair(CallbackInfo ci) {
        if (HUDCaching.renderingCacheOverride) {
            ci.cancel();
        }
    }
}
