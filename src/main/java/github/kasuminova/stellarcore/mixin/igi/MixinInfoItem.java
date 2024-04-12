package github.kasuminova.stellarcore.mixin.igi;

import com.github.lunatrius.ingameinfo.InGameInfoCore;
import com.github.lunatrius.ingameinfo.client.gui.overlay.InfoItem;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.IMixinInGameInfoCore;
import net.minecraft.client.renderer.OpenGlHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InfoItem.class)
public abstract class MixinInfoItem {

    @Shadow(remap = false) public abstract void drawInfo();

    @Inject(method = "drawInfo", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectDrawInfo(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.inGameInfoXML.hudFrameBuffer) {
            return;
        }
        if (!OpenGlHelper.framebufferSupported) {
            return;
        }
        IMixinInGameInfoCore instance = (IMixinInGameInfoCore) InGameInfoCore.INSTANCE;
        if (!instance.isPostDrawing()) {
            instance.addPostDrawAction(this::drawInfo);
            ci.cancel();
        }
    }

}
