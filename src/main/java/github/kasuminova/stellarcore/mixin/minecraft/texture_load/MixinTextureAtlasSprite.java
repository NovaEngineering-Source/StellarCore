package github.kasuminova.stellarcore.mixin.minecraft.texture_load;

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
        BufferedImage cache = SpriteBufferedImageCache.INSTANCE.get((TextureAtlasSprite) (Object) this);
        if (cache != null) {
            IOUtils.closeQuietly(imageStream);
            return cache;
        }
        return TextureUtil.readBufferedImage(imageStream);
    }

}
