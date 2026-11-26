package github.kasuminova.stellarcore.client.integration.enderutilities;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.common.model.TRSRTransformation;
import org.junit.jupiter.api.Test;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import static org.junit.jupiter.api.Assertions.*;

class NullifierQuadTransformTest {
    @Test
    void transformsGeometryAndCullFaceWhilePreservingTintTextureColorUvAndSourceOwnership() {
        final TextureAtlasSprite sprite = new TextureAtlasSprite("nullifier-transform-test") { };
        final VertexFormat format = DefaultVertexFormats.ITEM;
        final int[] source = new int[format.getIntegerSize() * 4];
        for (int vertex = 0; vertex < 4; vertex++) {
            final int at = vertex * format.getIntegerSize();
            source[at] = Float.floatToRawIntBits((vertex & 1) * .7F);
            source[at + 1] = Float.floatToRawIntBits((vertex >>> 1) * .5F);
            source[at + 2] = Float.floatToRawIntBits(.3F);
            source[at + 3] = 0xBBAA9988;
            source[at + 4] = Float.floatToRawIntBits(.23F);
            source[at + 5] = Float.floatToRawIntBits(.79F);
            source[at + 6] = 0x2A007F00;
        }
        final int[] original = source.clone();
        final Matrix4f matrix = new Matrix4f();
        matrix.rotZ((float) (Math.PI / 2));
        matrix.setScale(.6F);
        matrix.m03 = .2F;
        matrix.m13 = .4F;
        matrix.m23 = -.1F;
        final Matrix3f normalMatrix = new Matrix3f();
        matrix.getRotationScale(normalMatrix);
        normalMatrix.invert();
        normalMatrix.transpose();
        final int[] expected = source.clone();
        final float[] values = new float[4];
        for (int element = 0; element < format.getElementCount(); element++) {
            final VertexFormatElement.EnumUsage usage = format.getElement(element).getUsage();
            if (usage != VertexFormatElement.EnumUsage.POSITION && usage != VertexFormatElement.EnumUsage.NORMAL) continue;
            for (int vertex = 0; vertex < 4; vertex++) {
                LightUtil.unpack(original, values, format, vertex, element);
                if (usage == VertexFormatElement.EnumUsage.POSITION) {
                    final Vector4f position = new Vector4f(values[0], values[1], values[2], 1F);
                    matrix.transform(position);
                    position.get(values);
                } else {
                    final Vector3f normal = new Vector3f(values[0], values[1], values[2]);
                    normalMatrix.transform(normal);
                    normal.normalize();
                    normal.get(values);
                }
                LightUtil.pack(values, expected, format, vertex, element);
            }
        }
        final BakedQuad input = new BakedQuad(source, 3, EnumFacing.UP, sprite, true, format);
        final BakedQuad output = NullifierQuadTransform.transform(input, new TRSRTransformation(matrix));
        assertNotSame(input, output);
        assertNotSame(source, output.getVertexData());
        assertArrayEquals(original, source);
        assertArrayEquals(expected, output.getVertexData());
        assertEquals(EnumFacing.WEST, output.getFace());
        assertEquals(input.getTintIndex(), output.getTintIndex());
        assertSame(sprite, output.getSprite());
        assertSame(format, output.getFormat());
        assertEquals(input.shouldApplyDiffuseLighting(), output.shouldApplyDiffuseLighting());
    }
}
