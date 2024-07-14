package github.kasuminova.stellarcore.client.hudcaching;

import com.github.lunatrius.ingameinfo.handler.Ticker;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.minecraft.hudcaching.AccessorGuiIngameForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

public class HUDCaching {

    public static final HUDCaching INSTANCE = new HUDCaching();

    private static final Minecraft MC = Minecraft.getMinecraft();

    public static Framebuffer framebuffer = null;
    public static boolean dirty = true;
    public static boolean renderingCacheOverride = false;

    public static boolean igiRendering = false;

    private static long lastRenderMS = 0;

    private HUDCaching() {
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (!OpenGlHelper.isFramebufferEnabled() && MC.player != null) {
                StellarCoreConfig.PERFORMANCE.vanilla.hudCaching = false;
            }
        }
    }

    public static void renderCachedHud(EntityRenderer renderer, GuiIngame ingame, float partialTicks) {
        if (!OpenGlHelper.isFramebufferEnabled() || !StellarCoreConfig.PERFORMANCE.vanilla.hudCaching) {
            ingame.renderGameOverlay(partialTicks);
            return;
        }
        GlStateManager.enableDepth();
        ScaledResolution resolution = new ScaledResolution(MC);
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        renderer.setupOverlayRendering();
        GlStateManager.enableBlend();

        if (framebuffer != null) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder worldRenderer = tessellator.getBuffer();
            if (ingame instanceof GuiIngameForge) {
                //noinspection CastConflictsWithInstanceof
                ((AccessorGuiIngameForge) ingame).callRenderCrosshairs(partialTicks);
            } else if (GuiIngameForge.renderCrosshairs) {
                MC.getTextureManager().bindTexture(Gui.ICONS);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO);
                GlStateManager.enableAlpha();
                drawTexturedModalRect(tessellator, worldRenderer, (width >> 1) - 7, (height >> 1) - 7);
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            }

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1, 1, 1, 1);
            framebuffer.bindFramebufferTexture();
            drawTexturedRect(tessellator, worldRenderer, (float) resolution.getScaledWidth_double(), (float) resolution.getScaledHeight_double());
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        }

        int timeRange = 1000 / StellarCoreConfig.PERFORMANCE.vanilla.hudCachingFPSLimit;
        if (System.currentTimeMillis() - lastRenderMS > timeRange) {
            dirty = true;
            lastRenderMS = System.currentTimeMillis();
        }

        if (framebuffer == null || dirty) {
            dirty = false;
            (framebuffer = checkFramebufferSizes(framebuffer, MC.displayWidth, MC.displayHeight)).framebufferClear();
            framebuffer.bindFramebuffer(false);
            GlStateManager.disableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.disableLighting();
            GlStateManager.disableFog();
            renderingCacheOverride = true;
            ingame.renderGameOverlay(partialTicks);
            renderingCacheOverride = false;
            MC.getFramebuffer().bindFramebuffer(false);
            GlStateManager.enableBlend();
        }

        GlStateManager.enableDepth();

        if (Loader.isModLoaded("ingameinfoxml") && StellarCoreConfig.PERFORMANCE.inGameInfoXML.hudFrameBuffer) {
            renderIGIOverlay(partialTicks);
        }
    }

    @Optional.Method(modid = "ingameinfoxml")
    private static void renderIGIOverlay(float partialTicks) {
        igiRendering = true;
        Ticker.INSTANCE.onRenderTick(new TickEvent.RenderTickEvent(TickEvent.Phase.END, partialTicks));
        igiRendering = false;
    }

    private static Framebuffer checkFramebufferSizes(Framebuffer framebuffer, int width, int height) {
        if (framebuffer == null || framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
            if (framebuffer == null) {
                framebuffer = new Framebuffer(width, height, true);
                framebuffer.framebufferColor[0] = 0.0f;
                framebuffer.framebufferColor[1] = 0.0f;
                framebuffer.framebufferColor[2] = 0.0f;
            } else {
                framebuffer.createBindFramebuffer(width, height);
            }

            framebuffer.setFramebufferFilter(GL11.GL_NEAREST);
        }

        return framebuffer;
    }

    private static void drawTexturedRect(Tessellator tessellator, BufferBuilder buffer, float width, float height) {
        GlStateManager.enableTexture2D();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(0, height, 0.0).tex(0, 0).endVertex();
        buffer.pos(width, height, 0.0).tex(1, 0).endVertex();
        buffer.pos(width, 0, 0.0).tex(1, 1).endVertex();
        buffer.pos(0, 0, 0.0).tex(0, 1).endVertex();
        tessellator.draw();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    private static void drawTexturedModalRect(Tessellator tessellator, BufferBuilder buffer, int x, int y) {
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + 16, 100.0).tex(0.0f, 0.0625f).endVertex();
        buffer.pos(x + 16, y + 16, 100.0).tex(0.0625f, 0.0625f).endVertex();
        buffer.pos(x + 16, y, 100.0).tex(0.0625f, 0.0f).endVertex();
        buffer.pos(x, y, 100.0).tex(0.0f, 0.0f).endVertex();
        tessellator.draw();
    }
}
