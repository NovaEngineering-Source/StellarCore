package github.kasuminova.stellarcore.client.integration.enderutilities;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.Optional;

/**
 * Applies the original display placement to existing baked vertices without loading models.
 */
final class NullifierQuadTransform {
    // Actinium: PARTIAL, PARALLEL, ALIGNED, PASS_OPTIMIZABLE and the lazy-computation marker.
    private static final int DERIVED_FLAGS = Integer.MIN_VALUE | 1 | 2 | 4 | 32;

    private NullifierQuadTransform() {
    }

    static TRSRTransformation placement(final IModelState state) {
        final TRSRTransformation fromCenter = new TRSRTransformation(new Vector3f(-0.5F, -0.5F, -0.5F), null, null, null);
        final TRSRTransformation rotation = TRSRTransformation.from(ModelRotation.X0_Y180);
        final TRSRTransformation toCenter = new TRSRTransformation(new Vector3f(0.5F, 0.5F, 0.5F), null, null, null);
        final TRSRTransformation scale = new TRSRTransformation(null, null, new Vector3f(0.6F, 0.6F, 0.6F), null);
        return state.apply(Optional.empty()).orElse(TRSRTransformation.identity())
                    .compose(TRSRTransformation.blockCenterToCorner(fromCenter.compose(rotation).compose(toCenter).compose(scale)));
    }

    static BakedQuad transform(final BakedQuad source, final TRSRTransformation transform) {
        return transform(source, transform, source.getVertexData());
    }

    static BakedQuad transform(final BakedQuad source, final TRSRTransformation transform, final int[] sourceWords) {
        return new Scratch().transform(source, transform, sourceWords);
    }

    /** One miss owns these temporary values while building all seven face buckets. */
    static final class Scratch {
        private final float[] element = new float[4];
        private final Vector4f position = new Vector4f();
        private final Vector3f normal = new Vector3f();

        BakedQuad transform(final BakedQuad source, final TRSRTransformation transform, final int[] sourceWords) {
            final VertexFormat format = source.getFormat();
            final int[] words = sourceWords.clone();
            for (int index = 0; index < format.getElementCount(); index++) {
                switch (format.getElement(index).getUsage()) {
                    case POSITION: {
                        for (int vertex = 0; vertex < 4; vertex++) {
                            LightUtil.unpack(sourceWords, element, format, vertex, index);
                            if (format.getElement(index).getElementCount() < 4) element[3] = 1F;
                            position.set(element);
                            transform.transformPosition(position);
                            position.get(element);
                            LightUtil.pack(element, words, format, vertex, index);
                        }
                        break;
                    }
                    case NORMAL: {
                        for (int vertex = 0; vertex < 4; vertex++) {
                            LightUtil.unpack(sourceWords, element, format, vertex, index);
                            normal.set(element);
                            transform.transformNormal(normal);
                            normal.get(element);
                            LightUtil.pack(element, words, format, vertex, index);
                        }
                        break;
                    }
                    default: {
                    }
                }
            }
            final BakedQuad result = new BakedQuad(words, source.getTintIndex(), transform.rotate(source.getFace()), source.getSprite(),
                source.shouldApplyDiffuseLighting(), format);
            NullifierQuadFlags.copyMasked(source, result, ~DERIVED_FLAGS);
            return result;
        }
    }

    static int producerFlags(final int flags) {
        return flags & ~DERIVED_FLAGS;
    }
}
