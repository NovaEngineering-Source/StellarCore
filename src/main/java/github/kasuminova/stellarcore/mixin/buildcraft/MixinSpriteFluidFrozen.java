package github.kasuminova.stellarcore.mixin.buildcraft;

import buildcraft.lib.client.render.fluid.SpriteFluidFrozen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

@Mixin(value = SpriteFluidFrozen.class, remap = false)
public abstract class MixinSpriteFluidFrozen extends TextureAtlasSprite {

    @Shadow
    @Final
    public ResourceLocation srcLocation;

    protected MixinSpriteFluidFrozen(final String spriteName) {
        super(spriteName);
    }

    @Override
    public @Nonnull Collection<ResourceLocation> getDependencies() {
        // Let the atlas load the source's dependencies and generate its mipmaps first.
        // Otherwise BuildCraft calls custom source loaders directly on incomplete frames.
        return Collections.singletonList(srcLocation);
    }

}
