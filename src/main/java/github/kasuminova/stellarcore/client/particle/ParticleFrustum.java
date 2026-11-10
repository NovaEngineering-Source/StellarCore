package github.kasuminova.stellarcore.client.particle;

/**
 * A render-pass snapshot of inward-facing clip planes, in vertex coordinates.
 */
public final class ParticleFrustum {

    private final double[] planes = new double[24];
    private boolean valid;

    public void invalidate() {
        valid = false;
    }

    public void update(final float[][] source) {
        valid = false;
        if (source == null || source.length != 6) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            final float[] plane = source[i];
            if (plane == null || plane.length != 4) {
                return;
            }
            final double a = plane[0], b = plane[1], c = plane[2], d = plane[3];
            final double length = Math.sqrt(a * a + b * b + c * c);
            if (!(length > 0.0D) || !Double.isFinite(length) || !Double.isFinite(d)) {
                return;
            }
            final int offset = i * 4;
            planes[offset] = a / length;
            planes[offset + 1] = b / length;
            planes[offset + 2] = c / length;
            planes[offset + 3] = d / length;
        }
        valid = true;
    }

    /**
     * Only reject a sphere wholly outside a plane. Invalid data keeps the particle visible.
     */
    public boolean isOutside(final double x, final double y, final double z, final double radius) {
        if (!valid || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
            || !Double.isFinite(radius) || radius < 0.0D) {
            return false;
        }
        // Vertices are eventually converted to floats by the renderer. Keep a margin for that rounding.
        final double magnitude = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
        final double margin = Math.max(1.0E-3D, 4.0D * Math.ulp((float) magnitude));
        for (int offset = 0; offset < planes.length; offset += 4) {
            final double distance = planes[offset] * x + planes[offset + 1] * y
                + planes[offset + 2] * z + planes[offset + 3];
            if (distance < -radius - margin) {
                return true;
            }
        }
        return false;
    }
}
