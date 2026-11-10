package github.kasuminova.stellarcore.client.particle;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;

/**
 * Render-thread-only scope. Neither particle lists nor world data are visited here.
 */
public final class ParticleCulling {

    public static final ParticleCulling INSTANCE = new ParticleCulling();

    private final ClippingHelperImpl clippingHelper = new ClippingHelperImpl();
    private final ParticleFrustum frustum = new ParticleFrustum();
    private Thread renderThread;
    private boolean prepared;

    private ParticleCulling() {
    }

    public void begin() {
        end();
        if (StellarCoreConfig.PERFORMANCE.vanilla.particleCulling) {
            renderThread = Thread.currentThread();
        }
    }

    public void end() {
        renderThread = null;
        prepared = false;
        frustum.invalidate();
    }

    public boolean isActive() {
        return renderThread == Thread.currentThread()
            && StellarCoreConfig.PERFORMANCE.vanilla.particleCulling;
    }

    public boolean isOutside(final double x, final double y, final double z, final double radius) {
        if (!isActive()) {
            return false;
        }
        if (!prepared) {
            // Empty passes and passes containing only custom renderers never read GL matrices.
            // A private helper leaves vanilla's shared ClippingHelperImpl singleton unchanged.
            // Live matrices already include third-person and camera effects.
            clippingHelper.init();
            frustum.update(clippingHelper.frustum);
            prepared = true;
        }
        return frustum.isOutside(x, y, z, radius);
    }
}
