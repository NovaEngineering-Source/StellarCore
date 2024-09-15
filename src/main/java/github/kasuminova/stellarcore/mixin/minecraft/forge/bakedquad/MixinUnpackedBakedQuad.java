package github.kasuminova.stellarcore.mixin.minecraft.forge.bakedquad;

import github.kasuminova.stellarcore.client.pool.StellarUnpackedDataPool;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UnpackedBakedQuad.class)
public class MixinUnpackedBakedQuad {

    @Final
    @Mutable
    @Shadow(remap = false)
    protected float[][][] unpackedData;

    @SuppressWarnings("ConstantValue")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final float[][][] unpackedData, final int tint, final EnumFacing orientation, final TextureAtlasSprite texture, final boolean applyDiffuseLighting, final VertexFormat format, final CallbackInfo ci) {
        if ((Object) (this).getClass() == UnpackedBakedQuad.class) {
            this.unpackedData = StellarUnpackedDataPool.canonicalize(unpackedData);
        }
    }

}
