package github.kasuminova.stellarcore.mixin.minecraft.stitcher;

import net.minecraft.client.renderer.texture.Stitcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Stitcher.class)
public interface AccessorStitcher {

    @Accessor
    int getMipmapLevelStitcher();

    @Accessor
    int getMaxTileDimension();

}
