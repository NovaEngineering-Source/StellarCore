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

    /**
     * What {@code unpackedData} becomes once the quad has been packed.
     *
     * <p>Not {@code null}, even though the data is deliberately dropped: the
     * field has no getter, but it is reachable by reflection, and mods do reach
     * it. FoamFix's model deduplicator walks every baked model on the bake event
     * and takes the length of this array, so a {@code null} makes it throw once
     * per quad — and its handler prints the whole stack trace at INFO. A pack
     * with a hundred thousand quads then writes a hundred thousand stack traces
     * from the loading thread: the progress bar sits still for minutes and the
     * log grows past a gigabyte. An empty array reads as "this quad has no
     * unpacked vertices", which is exactly true by then, and iterating it costs
     * nothing.</p>
     */
    @Unique
    private static final float[][][] STELLAR_CORE$DROPPED_DATA = new float[0][0][0];

    /** Whether this quad's unpacked data was dropped, so the packed data must be used instead. */
    @Unique
    private boolean stellar_core$dropped;

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
        if (!stellar_core$dropped) {
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
                unpackedData = STELLAR_CORE$DROPPED_DATA;
                stellar_core$dropped = true;
            }
            ((AccessorBakedQuad) (Object) this).stellar_core$setVertexData(packedData);
            StellarUnpackedDataPool.canonicalizeAsync(packedData, canonicalized ->
                    ((AccessorBakedQuad) (Object) this).stellar_core$setVertexData(canonicalized));
        }
    }

}
