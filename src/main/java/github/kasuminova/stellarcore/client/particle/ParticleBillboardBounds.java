package github.kasuminova.stellarcore.client.particle;

/**
 * Conservative bounds for the quad emitted by Particle#renderParticle, without temporary vectors.
 */
public final class ParticleBillboardBounds {

    private ParticleBillboardBounds() {
    }

    public static double radius(final float scale, final float rotationX, final float rotationZ,
                                final float rotationYZ, final float rotationXY, final float rotationXZ,
                                final double quaternionLengthSquared) {
        if (!Double.isFinite(quaternionLengthSquared) || quaternionLengthSquared < 0.0D) {
            return Double.NaN;
        }
        // The L1 sum bounds all four corner lengths even for a non-unit camera basis.
        final float halfSize = 0.1F * scale;
        final double extent = Math.abs((double) halfSize * rotationX)
            + Math.abs((double) halfSize * rotationZ)
            + Math.abs((double) halfSize * rotationYZ)
            + Math.abs((double) halfSize * rotationXY)
            + Math.abs((double) halfSize * rotationXZ);
        // A non-unit quaternion scales vector lengths by its squared norm.
        // Use the actual float/LUT quaternion, with a small allowance for vertex rounding.
        return extent * quaternionLengthSquared * 1.00001D;
    }
}
