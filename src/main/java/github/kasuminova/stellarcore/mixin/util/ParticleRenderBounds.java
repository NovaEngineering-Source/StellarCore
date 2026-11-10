package github.kasuminova.stellarcore.mixin.util;

/**
 * Implemented on Particle; reads the protected fields used by its base renderer.
 */
public interface ParticleRenderBounds {

    boolean stellar_core$isBillboardOutside(float partialTicks, float rotationX, float rotationZ,
                                            float rotationYZ, float rotationXY, float rotationXZ);
}
