package github.kasuminova.stellarcore.mixin.igi;

import com.github.lunatrius.ingameinfo.InGameInfoCore;
import com.github.lunatrius.ingameinfo.client.gui.overlay.Info;
import github.kasuminova.stellarcore.client.util.RenderUtils;
import github.kasuminova.stellarcore.mixin.util.IMixinInGameInfoCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedList;
import java.util.List;

@Mixin(InGameInfoCore.class)
public class MixinInGameInfoCore implements IMixinInGameInfoCore {

    @Unique
    private final List<Runnable> stellarcore$postDrawList = new LinkedList<>();

    @Shadow(remap = false) @Final private List<Info> info;
    @Unique
    private Framebuffer stellarcore$fbo = null;
    @Unique
    private boolean stellarcore$refreshFBO = true;
    @Unique
    private boolean stellarcore$postDrawing = true;

    @Unique
    private int stellarcore$tickCounter = 0;
    @Unique
    private int stellarcore$displayWidth = 0;
    @Unique
    private int stellarcore$displayHeight = 0;

    @Inject(method = "onTickClient", at = @At("HEAD"), remap = false)
    private void onTickClient(final CallbackInfo ci) {
        stellarcore$tickCounter++;
        if (stellarcore$tickCounter % 2 == 0) {
            stellarcore$refreshFBO = true;
        }
    }

    /**
     * @author Kasumi_Nova
     * @reason 使用 FBO 优化 IGI 渲染性能，帧率越高效果越好。
     */
    @Overwrite(remap = false)
    public void onTickRender() {
        if (!OpenGlHelper.framebufferSupported) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        stellarcore$postDrawing = false;

        if (stellarcore$fbo == null) {
            stellarcore$displayWidth = minecraft.displayWidth;
            stellarcore$displayHeight = minecraft.displayHeight;
            stellarcore$fbo = new Framebuffer(stellarcore$displayWidth, stellarcore$displayHeight, false);
            stellarcore$fbo.framebufferColor[0] = 0.0F;
            stellarcore$fbo.framebufferColor[1] = 0.0F;
            stellarcore$fbo.framebufferColor[2] = 0.0F;
        }

        if (stellarcore$refreshFBO) {
            stellarcore$postDrawList.clear();
            stellar_core$renderToFBO(minecraft);
            stellarcore$refreshFBO = false;
        }

        RenderUtils.renderFramebuffer(minecraft, stellarcore$fbo);
        stellarcore$postDrawing = true;
        stellarcore$postDrawList.forEach(Runnable::run);
    }

    @Unique
    private void stellar_core$renderToFBO(final Minecraft minecraft) {
        if (stellarcore$displayWidth != minecraft.displayWidth || stellarcore$displayHeight != minecraft.displayHeight) {
            stellarcore$displayWidth = minecraft.displayWidth;
            stellarcore$displayHeight = minecraft.displayHeight;
            stellarcore$fbo.createBindFramebuffer(stellarcore$displayWidth, stellarcore$displayHeight);
        } else {
            stellarcore$fbo.framebufferClear();
        }
        stellarcore$fbo.bindFramebuffer(false);

        GlStateManager.disableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        info.forEach(Info::draw);

        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void addPostDrawAction(final Runnable action) {
        stellarcore$postDrawList.add(action);
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public boolean isPostDrawing() {
        return stellarcore$postDrawing;
    }

}