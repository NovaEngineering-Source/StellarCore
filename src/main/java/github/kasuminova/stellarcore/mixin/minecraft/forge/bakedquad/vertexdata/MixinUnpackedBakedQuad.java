package github.kasuminova.stellarcore.mixin.minecraft.forge.bakedquad.vertexdata;

import github.kasuminova.stellarcore.client.pool.StellarUnpackedDataPool;
import github.kasuminova.stellarcore.mixin.util.AccessorBakedQuad;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
    private void injectInit(final float[][][] unpackedData, final int tint, final EnumFacing orientation, final TextureAtlasSprite texture, final boolean applyDiffuseLighting, final VertexFormat format, final CallbackInfo ci) {
        // Deallocate vertex data.
        ((AccessorBakedQuad) (Object) this).stellar_core$setVertexData(null);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/vertex/VertexFormat;getSize()I"))
    private static int injectInitAtInvoke(final VertexFormat instance) {
        // Return 0 to create empty array, don't allocate.
        return 0;
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    @Inject(method = "getVertexData", at = @At("HEAD"))
    private void injectGetVertexData(final CallbackInfoReturnable<int[]> cir) {
        if (packed) {
            return;
        }
        synchronized (unpackedData) {
            if (packed) {
                return;
            }
            // Lazy init array
            ((AccessorBakedQuad) (Object) this).stellar_core$setVertexData(new int[format.getSize()]);
            for (int v = 0; v < 4; v++) {
                for (int e = 0; e < format.getElementCount(); e++) {
                    LightUtil.pack(unpackedData[v][e], vertexData, format, v, e);
                }
            }
            ((AccessorBakedQuad) (Object) this).stellar_core$setVertexData(StellarUnpackedDataPool.canonicalize(vertexData));
            packed = true;
        }
    }

}
