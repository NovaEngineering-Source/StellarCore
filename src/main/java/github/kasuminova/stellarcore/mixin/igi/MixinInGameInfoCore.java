package github.kasuminova.stellarcore.mixin.igi;

import com.github.lunatrius.ingameinfo.InGameInfoCore;
import com.github.lunatrius.ingameinfo.client.gui.overlay.Info;
import com.github.lunatrius.ingameinfo.handler.ConfigurationHandler;
import github.kasuminova.stellarcore.client.util.RenderUtils;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.IMixinInGameInfoCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;

@Mixin(InGameInfoCore.class)
public class MixinInGameInfoCore implements IMixinInGameInfoCore {

    @Unique
    private final List<Runnable> stellar_core$postDrawList = new ObjectArrayList<>();

    @Shadow(remap = false) @Final private List<Info> info;

    @Unique
    private Framebuffer stellar_core$fbo = null;

    @Unique
    private boolean stellar_core$refreshFBO = true;
    @Unique
    private boolean stellar_core$postDrawing = true;

    @Unique
    private int stellar_core$displayWidth = 0;
    @Unique
    private int stellar_core$displayHeight = 0;

    @Unique
    private long stellar_core$lastRenderMS = 0;

    /**
     * @author Kasumi_Nova
     * @reason 使用 FBO 优化 IGI 渲染性能，帧率越低效果越好。
     */
    @Inject(method = "onTickRender", at = @At("HEAD"), cancellable = true, remap = false)
    public void onTickRender(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.inGameInfoXML.hudFrameBuffer) {
            stellar_core$clearRenderCache();
            return;
        }
        if (!OpenGlHelper.isFramebufferEnabled()) {
            stellar_core$clearRenderCache();
            return;
        }
        ci.cancel();

        int timeRange = 1000 / StellarCoreConfig.PERFORMANCE.inGameInfoXML.hudFrameRate;
        if (System.currentTimeMillis() - stellar_core$lastRenderMS > timeRange) {
            stellar_core$refreshFBO = true;
            stellar_core$lastRenderMS = System.currentTimeMillis();
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (stellar_core$fbo == null) {
            stellar_core$displayWidth = minecraft.displayWidth;
            stellar_core$displayHeight = minecraft.displayHeight;
            stellar_core$fbo = new Framebuffer(stellar_core$displayWidth, stellar_core$displayHeight, false);
            stellar_core$fbo.framebufferColor[0] = 0.0F;
            stellar_core$fbo.framebufferColor[1] = 0.0F;
            stellar_core$fbo.framebufferColor[2] = 0.0F;
        }

        if (stellar_core$refreshFBO) {
            stellar_core$postDrawList.clear();
            stellar_core$postDrawing = false;
            GlStateManager.pushMatrix();
            try {
                stellar_core$renderToFBO(minecraft);
                stellar_core$refreshFBO = false;
            } finally {
                GlStateManager.popMatrix();
                minecraft.getFramebuffer().bindFramebuffer(false);
                stellar_core$postDrawing = true;
            }
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        RenderUtils.renderFramebuffer(minecraft, stellar_core$fbo);
        stellar_core$postDrawing = true;
        stellar_core$postDrawList.forEach(Runnable::run);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Unique
    private void stellar_core$renderToFBO(final Minecraft minecraft) {
        if (stellar_core$displayWidth != minecraft.displayWidth || stellar_core$displayHeight != minecraft.displayHeight) {
            stellar_core$displayWidth = minecraft.displayWidth;
            stellar_core$displayHeight = minecraft.displayHeight;
            stellar_core$fbo.createBindFramebuffer(stellar_core$displayWidth, stellar_core$displayHeight);
        } else {
            stellar_core$fbo.framebufferClear();
        }
        stellar_core$fbo.bindFramebuffer(false);

        GlStateManager.disableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(ConfigurationHandler.scale, ConfigurationHandler.scale, ConfigurationHandler.scale);
            info.forEach(Info::draw);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void stellar_core$clearRenderCache() {
        if (stellar_core$fbo != null) {
            RenderUtils.deleteOwnedFramebuffer(Minecraft.getMinecraft(), stellar_core$fbo);
            stellar_core$fbo = null;
        }
        stellar_core$postDrawList.clear();
        stellar_core$refreshFBO = true;
        stellar_core$postDrawing = true;
        stellar_core$lastRenderMS = 0;
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void addPostDrawAction(final Runnable action) {
        stellar_core$postDrawList.add(action);
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public boolean isPostDrawing() {
        return stellar_core$postDrawing;
    }

}
