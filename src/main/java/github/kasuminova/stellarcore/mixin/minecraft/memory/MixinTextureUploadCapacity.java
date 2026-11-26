package github.kasuminova.stellarcore.mixin.minecraft.memory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

@Mixin(TextureUtil.class)
public abstract class MixinTextureUploadCapacity {
    @Unique
    private static final int stellar_core$uploadPixels = 64 * 1024;
    @Shadow
    @Final
    @Mutable
    private static IntBuffer DATA_BUFFER;

    @Shadow
    public static int anaglyphColor(final int pixel) {
        throw new AssertionError();
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 4194304), require = 1)
    private static int stellar_core$smallUploadBuffer(final int original) {
        return stellar_core$uploadPixels;
    }

    @ModifyConstant(method = "uploadTextureSub", constant = @Constant(intValue = 4194304), require = 1)
    private static int stellar_core$boundedArrayUpload(final int original, final int level, final int[] pixels,
                                               final int width, final int height, final int x, final int y,
                                               final boolean blur, final boolean clamp, final boolean mipmap) {
        return stellar_core$ensureUploadCapacity(width);
    }

    @ModifyConstant(method = "uploadTextureImageSubImpl", constant = @Constant(intValue = 4194304), require = 1)
    private static int stellar_core$boundedImageUpload(final int original, final BufferedImage image,
                                               final int x, final int y, final boolean blur, final boolean clamp) {
        return stellar_core$ensureUploadCapacity(image.getWidth());
    }

    @Unique
    private static int stellar_core$ensureUploadCapacity(final int width) {
        final int capacity = Math.max(stellar_core$uploadPixels, width);
        if (DATA_BUFFER.capacity() < capacity) DATA_BUFFER = GLAllocation.createDirectIntBuffer(capacity);
        return capacity;
    }

    /**
     * @author StellarCore
     * @reason Avoid re-converting and allocating the complete source image for every anaglyph chunk.
     */
    @Overwrite
    private static void copyToBufferPos(final int[] pixels, final int offset, final int length) {
        DATA_BUFFER.clear();
        if (Minecraft.getMinecraft().gameSettings.anaglyph) {
            for (int i = offset; i < offset + length; i++) DATA_BUFFER.put(anaglyphColor(pixels[i]));
        } else {
            DATA_BUFFER.put(pixels, offset, length);
        }
        DATA_BUFFER.flip();
    }
}
