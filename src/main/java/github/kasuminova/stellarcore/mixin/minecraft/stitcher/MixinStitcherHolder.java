package github.kasuminova.stellarcore.mixin.minecraft.stitcher;

import github.kasuminova.stellarcore.mixin.util.AccessorStitcherHolder;
import net.minecraft.client.renderer.texture.Stitcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(Stitcher.Holder.class)
public class MixinStitcherHolder implements AccessorStitcherHolder {

    @Final
    @Shadow
    private int width;

    @Final
    @Shadow
    private int height;

    @Shadow
    private float scaleFactor;

    @Override
    public int realWidth() {
        return width;
    }

    @Override
    public int realHeight() {
        return height;
    }

    @Override
    public float scaleFactor() {
        return scaleFactor;
    }

}
