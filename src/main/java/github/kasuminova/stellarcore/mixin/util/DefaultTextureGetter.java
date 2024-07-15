package github.kasuminova.stellarcore.mixin.util;

import com.google.common.base.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class DefaultTextureGetter implements Function<ResourceLocation, TextureAtlasSprite> {
    @Nonnull
    @Override
    public TextureAtlasSprite apply(final ResourceLocation input) {
        return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(input.toString());
    }
}
