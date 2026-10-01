package github.kasuminova.stellarcore.client.model.obj;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.TRSRTransformation;

import javax.vecmath.Matrix4f;
import java.util.Arrays;
import java.util.function.Function;

public final class OBJBakeCacheKey {

    private final VertexFormat format;
    private final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter;
    private final float[] transform;
    private final float[] cameraTransforms;
    private final long visibility;
    private final int hash;

    public OBJBakeCacheKey(final VertexFormat format,
                           final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
                           final TRSRTransformation transform,
                           final float[] cameraTransforms,
                           final long visibility) {
        this.format = format;
        this.bakedTextureGetter = bakedTextureGetter;
        this.transform = transform == null ? null : toArray(transform.getMatrix());
        this.cameraTransforms = cameraTransforms;
        this.visibility = visibility;
        int hash = 31 * System.identityHashCode(format) + System.identityHashCode(bakedTextureGetter);
        hash = 31 * hash + Long.hashCode(visibility);
        hash = 31 * hash + Arrays.hashCode(this.transform);
        this.hash = 31 * hash + Arrays.hashCode(cameraTransforms);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OBJBakeCacheKey other)) {
            return false;
        }
        return this.hash == other.hash
                && this.format == other.format
                && this.bakedTextureGetter == other.bakedTextureGetter
                && this.visibility == other.visibility
                && Arrays.equals(this.transform, other.transform)
                && Arrays.equals(this.cameraTransforms, other.cameraTransforms);
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    private static float[] toArray(final Matrix4f matrix) {
        return new float[]{
                matrix.m00, matrix.m01, matrix.m02, matrix.m03,
                matrix.m10, matrix.m11, matrix.m12, matrix.m13,
                matrix.m20, matrix.m21, matrix.m22, matrix.m23,
                matrix.m30, matrix.m31, matrix.m32, matrix.m33
        };
    }

}
