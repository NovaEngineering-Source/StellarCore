package github.kasuminova.stellarcore.mixin.minecraft.forge.bakedquad.vertexdata;

import github.kasuminova.stellarcore.client.pool.StellarUnpackedDataPool;
import github.kasuminova.stellarcore.mixin.util.AccessorBakedQuad;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("RedundantCast")
@Mixin(UnpackedBakedQuad.class)
public class MixinUnpackedBakedQuad extends BakedQuad {

    @Final
    @Mutable
    @Shadow(remap = false)
    protected float[][][] unpackedData;

    @Shadow(remap = false)
    protected boolean packed;

    @Final
    @Shadow(remap = false)
    protected VertexFormat format;

    @SuppressWarnings({"deprecation", "DataFlowIssue"})
    public MixinUnpackedBakedQuad() {
        super(null, 0, null, null);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final float[][][] unpackedDataIn, final int tint, final EnumFacing orientation, final TextureAtlasSprite texture, final boolean applyDiffuseLighting, final VertexFormat formatIn, final CallbackInfo ci) {
        if ((Object) this.getClass() != UnpackedBakedQuad.class) {
            return;
        }
        stellar_core$packAndDrop();
    }

    @Inject(method = "pipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void stellar_core$pipePacked(final IVertexConsumer consumer, final CallbackInfo ci) {
        if (unpackedData != null) {
            return;
        }
        LightUtil.putBakedQuad(consumer, (BakedQuad) (Object) this);
        ci.cancel();
    }

    @Inject(method = "getVertexData", at = @At("HEAD"))
    private void injectGetVertexData(final CallbackInfoReturnable<int[]> cir) {
        if (packed) {
            return;
        }
        stellar_core$packAndDrop();
    }

    @Unique
    private void stellar_core$packAndDrop() {
        final float[][][] data = unpackedData;
        if (data == null) {
            packed = true;
            return;
        }
        synchronized (data) {
            if (packed) {
                return;
            }
            int[] packedData = vertexData;
            if (packedData == null || packedData.length < format.getSize()) {
                packedData = new int[format.getSize()];
            }
            for (int v = 0; v < 4; v++) {
                for (int e = 0; e < format.getElementCount(); e++) {
                    LightUtil.pack(data[v][e], packedData, format, v, e);
                }
            }
            packed = true;
            if ((Object) this.getClass() == UnpackedBakedQuad.class) {
                unpackedData = null;
            }
            ((AccessorBakedQuad) (Object) this).stellar_core$setVertexData(packedData);
            StellarUnpackedDataPool.canonicalizeAsync(packedData, canonicalized ->
                    ((AccessorBakedQuad) (Object) this).stellar_core$setVertexData(canonicalized));
        }
    }

}
