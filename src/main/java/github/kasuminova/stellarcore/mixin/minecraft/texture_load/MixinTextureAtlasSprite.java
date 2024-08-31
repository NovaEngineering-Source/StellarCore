package github.kasuminova.stellarcore.mixin.minecraft.texture_load;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.client.texture.SpriteBufferedImageCache;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

@Mixin(TextureAtlasSprite.class)
public class MixinTextureAtlasSprite {

    @Redirect(
            method = "loadSpriteFrames",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureUtil;readBufferedImage(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;"
            )
    )
    private BufferedImage redirectLoadSpriteFramesRead(final InputStream imageStream) throws IOException {
        BufferedImage cache = SpriteBufferedImageCache.INSTANCE.getImage((TextureAtlasSprite) (Object) this);
        if (cache != null) {
            IOUtils.closeQuietly(imageStream);
            return cache;
        }
        return TextureUtil.readBufferedImage(imageStream);
    }

    @Redirect(method = "loadSpriteFrames", at = @At(value = "INVOKE", target = "Ljava/awt/image/BufferedImage;getRGB(IIII[III)[I"))
    private int[] modifyLoadSpriteFramesgetRGB(final BufferedImage instance,
                                               final int startX, final int startY, final int width, final int height,
                                               final int[] output, final int offset, final int size,
                                               @Local(name = "aint") int[][] aint)
    {
        int[] cache = SpriteBufferedImageCache.INSTANCE.getRGBAndRemove((TextureAtlasSprite) (Object) this);
        if (cache != null) {
            int exceptedSize = instance.getWidth() * instance.getHeight();
            if (cache.length != exceptedSize) {
                return instance.getRGB(startX, startY, width, height, output, offset, size);
            }

            aint[0] = cache;
            return cache;
        }
        return instance.getRGB(startX, startY, width, height, output, offset, size);
    }

}
