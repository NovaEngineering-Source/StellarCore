package github.kasuminova.stellarcore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

public class RenderUtils {

    public static void renderFramebuffer(final Minecraft minecraft, final Framebuffer framebuffer) {
        ScaledResolution res = new ScaledResolution(minecraft);
        double scaledHeight = res.getScaledHeight_double();
        double scaledWidth = res.getScaledWidth_double();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        framebuffer.bindFramebufferTexture();
        GlStateManager.enableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(0, scaledHeight, 0.0).tex(0, 0).endVertex();
        buffer.pos(scaledWidth, scaledHeight, 0.0).tex(1, 0).endVertex();
        buffer.pos(scaledWidth, 0, 0.0).tex(1, 1).endVertex();
        buffer.pos(0, 0, 0.0).tex(0, 1).endVertex();
        tessellator.draw();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }

}
