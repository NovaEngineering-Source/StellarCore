package github.kasuminova.stellarcore.mixin.minecraft.bakedquad;

import github.kasuminova.stellarcore.client.pool.StellarUnpackedDataPool;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BakedQuad.class)
public class MixinBakedQuad {

    @Final
    @Shadow
    @Mutable
    protected int[] vertexData;

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "<init>([IILnet/minecraft/util/EnumFacing;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;ZLnet/minecraft/client/renderer/vertex/VertexFormat;)V",
            at = @At("RETURN")
    )
    private void injectInit(final int[] vertexDataIn, final int tintIndexIn, final EnumFacing faceIn, final TextureAtlasSprite spriteIn, final boolean applyDiffuseLighting, final VertexFormat format, final CallbackInfo ci) {
        if ((Object) (this).getClass() == BakedQuad.class && vertexData != null && vertexData.length > 0) {
            StellarUnpackedDataPool.canonicalizeAsync(vertexData, (canonicalizedData) -> vertexData = canonicalizedData);
        }
    }

}
