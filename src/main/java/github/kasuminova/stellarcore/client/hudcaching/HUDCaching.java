package github.kasuminova.stellarcore.client.hudcaching;

import com.github.lunatrius.ingameinfo.handler.Ticker;
import github.kasuminova.stellarcore.client.util.RenderUtils;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.mod.Mods;
import github.kasuminova.stellarcore.mixin.minecraft.hudcaching.AccessorGuiIngameForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.client.GuiIngameForge;
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
    private static boolean lastMinimapVisible = true;

    public static boolean igiRendering = false;

    private static long lastRenderMS = 0;

    private HUDCaching() {
    }

    public static void releaseFramebuffer() {
        if (framebuffer != null) {
            RenderUtils.deleteOwnedFramebuffer(MC, framebuffer);
            framebuffer = null;
        }
        dirty = true;
        lastRenderMS = 0;
    }

    public static void releaseUnusedFramebuffer() {
        if (framebuffer != null && (MC.world == null
                || !OpenGlHelper.isFramebufferEnabled()
                || !StellarCoreConfig.PERFORMANCE.vanilla.hudCaching || isCurrentDimensionBlacklisted())) {
            releaseFramebuffer();
        }
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
        // JourneyMap 6.0 draws its minimap only in-world and in chat. Refresh the HUD texture
        // when that visibility changes, and do not blit the previous screen's cached HUD first.
        final boolean minimapVisible = MC.currentScreen == null || MC.currentScreen instanceof GuiChat;
        final boolean minimapVisibilityChanged = minimapVisible != lastMinimapVisible;
        if (minimapVisibilityChanged) {
            lastMinimapVisible = minimapVisible;
            dirty = true;
        }

        if (!OpenGlHelper.isFramebufferEnabled() || !StellarCoreConfig.PERFORMANCE.vanilla.hudCaching) {
            releaseFramebuffer();
            ingame.renderGameOverlay(partialTicks);
            renderIGIOverlayIfRequiredWhenBypassed(partialTicks);
            return;
        }

        if (isCurrentDimensionBlacklisted()) {
            releaseFramebuffer();
            ingame.renderGameOverlay(partialTicks);
            renderIGIOverlayIfRequiredWhenBypassed(partialTicks);
            return;
        }

        GlStateManager.enableDepth();
        ScaledResolution resolution = new ScaledResolution(MC);
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        renderer.setupOverlayRendering();
        GlStateManager.enableBlend();

        if (framebuffer != null && !minimapVisibilityChanged) {
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
            final boolean previousOverride = renderingCacheOverride;
            // Keep literal boolean stores so older LaunchWrapper compatibility transformers can
            // mirror the cache window without depending on the saved local-variable value.
            renderingCacheOverride = true;
            try {
                ingame.renderGameOverlay(partialTicks);
            } finally {
                if (previousOverride) {
                    renderingCacheOverride = true;
                } else {
                    renderingCacheOverride = false;
                }
                MC.getFramebuffer().bindFramebuffer(false);
                GlStateManager.enableBlend();
            }
        }

        GlStateManager.enableDepth();

        if (Mods.INGAME_INFO_XML.loaded() && StellarCoreConfig.PERFORMANCE.inGameInfoXML.hudFrameBuffer) {
            renderIGIOverlay(partialTicks);
        }
    }

    private static void renderIGIOverlayIfRequiredWhenBypassed(float partialTicks) {
        if (StellarCoreConfig.PERFORMANCE.vanilla.hudCaching && Mods.INGAME_INFO_XML.loaded() && StellarCoreConfig.PERFORMANCE.inGameInfoXML.hudFrameBuffer) {
            renderIGIOverlay(partialTicks);
        }
    }

    private static boolean isCurrentDimensionBlacklisted() {
        if (MC.world == null || MC.world.provider == null) {
            return false;
        }

        int currentDim = MC.world.provider.getDimension();
        int[] blacklist = StellarCoreConfig.PERFORMANCE.vanilla.hudCachingDimensionBlacklist;
        if (blacklist == null) {
            return false;
        }

        for (int dimId : blacklist) {
            if (dimId == currentDim) {
                return true;
            }
        }
        return false;
    }

    @Optional.Method(modid = "ingameinfoxml")
    private static void renderIGIOverlay(float partialTicks) {
        final boolean previousRendering = igiRendering;
        igiRendering = true;
        try {
            Ticker.INSTANCE.onRenderTick(new TickEvent.RenderTickEvent(TickEvent.Phase.END, partialTicks));
        } finally {
            igiRendering = previousRendering;
        }
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
        // The owned texture's filter is set when its framebuffer is created or resized.
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(0, height, 0.0).tex(0, 0).endVertex();
        buffer.pos(width, height, 0.0).tex(1, 0).endVertex();
        buffer.pos(width, 0, 0.0).tex(1, 1).endVertex();
        buffer.pos(0, 0, 0.0).tex(0, 1).endVertex();
        tessellator.draw();
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
