package github.kasuminova.stellarcore.client.model.obj;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.common.model.TRSRTransformation;

import javax.vecmath.Matrix4f;
import java.util.Arrays;

public final class OBJBakeCacheKey {

    private final VertexFormat format;
    private final float[] transform;
    private final long visibility;
    private final int hash;

    public OBJBakeCacheKey(final VertexFormat format, final TRSRTransformation transform, final long visibility) {
        this.format = format;
        this.transform = transform == null ? null : toArray(transform.getMatrix());
        this.visibility = visibility;
        int hash = 31 * System.identityHashCode(format) + Long.hashCode(visibility);
        this.hash = 31 * hash + Arrays.hashCode(this.transform);
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
                && this.visibility == other.visibility
                && Arrays.equals(this.transform, other.transform);
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
