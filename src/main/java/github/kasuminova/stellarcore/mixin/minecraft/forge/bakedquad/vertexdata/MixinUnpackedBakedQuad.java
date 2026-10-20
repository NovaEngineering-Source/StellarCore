package github.kasuminova.stellarcore.mixin.minecraft.forge.bakedquad.vertexdata;

import github.kasuminova.stellarcore.client.pool.StellarUnpackedDataPool;
import github.kasuminova.stellarcore.mixin.util.AccessorBakedQuad;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
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
    private void injectInit(final float[][][] unpackedData, final int tint, final EnumFacing orientation, final TextureAtlasSprite texture, final boolean applyDiffuseLighting, final VertexFormat format, final CallbackInfo ci) {
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
        stellar_core$putPackedQuad(consumer);
        ci.cancel();
    }

    @Inject(method = "getVertexData", at = @At("HEAD"))
    private void injectGetVertexData(final CallbackInfoReturnable<int[]> cir) {
        if (packed) {
            return;
        }
        stellar_core$packAndDrop();
    }

    /**
     * Forge's packed pipe helper reconstructs a three-component POSITION with
     * a fourth component of zero. UnpackedBakedQuad.Builder normally carries
     * that component as one, and model transforms rely on it for translation.
     * Keep the packed fast path, but restore the homogeneous coordinate before
     * forwarding the data.
     */
    @Unique
    private void stellar_core$putPackedQuad(final IVertexConsumer consumer) {
        if (this.hasTintIndex()) {
            consumer.setQuadTint(this.getTintIndex());
        }
        consumer.setTexture(this.getSprite());
        consumer.setApplyDiffuseLighting(this.shouldApplyDiffuseLighting());
        consumer.setQuadOrientation(this.getFace());

        final VertexFormat formatFrom = consumer.getVertexFormat();
        final VertexFormat formatTo = this.getFormat();
        final int countTo = formatTo.getElementCount();
        final int[] elementMap = LightUtil.mapFormats(formatFrom, formatTo);
        final float[] data = new float[4];
        for (int vertex = 0; vertex < 4; vertex++) {
            for (int element = 0; element < formatFrom.getElementCount(); element++) {
                final int sourceElement = elementMap[element];
                if (sourceElement == countTo) {
                    consumer.put(element);
                    continue;
                }
                LightUtil.unpack(this.getVertexData(), data, formatTo, vertex, sourceElement);
                final VertexFormatElement source = formatTo.getElement(sourceElement);
                if (source.isPositionElement() && source.getElementCount() < 4) {
                    data[3] = 1.0F;
                }
                consumer.put(element, data);
            }
        }
    }

    @Unique
    private void stellar_core$dropUnpackedData() {
        if ((Object) this.getClass() != UnpackedBakedQuad.class || stellar_core$dropped) {
            return;
        }
        unpackedData = STELLAR_CORE$DROPPED_DATA;
        stellar_core$dropped = true;
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
            ((AccessorBakedQuad) (Object) this).stellar_core$setVertexData(packedData);
            StellarUnpackedDataPool.canonicalizeAsync(packedData, canonicalized ->
                    ((AccessorBakedQuad) (Object) this).stellar_core$setVertexData(canonicalized));
            stellar_core$dropUnpackedData();
        }
    }

}
