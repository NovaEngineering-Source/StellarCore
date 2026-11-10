package github.kasuminova.stellarcore.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Cache by class so arbitrary renderer overrides are preserved without repeated reflection.
 */
public final class ParticleRenderers {

    private static final Class<?>[] PARAMETERS = {BufferBuilder.class, Entity.class, float.class,
        float.class, float.class, float.class, float.class, float.class};
    private static final String RENDER_METHOD = findRenderMethod();
    private static final ClassValue<Boolean> BASE_RENDERER = new ClassValue<>() {
        @Override
        protected Boolean computeValue(@Nonnull final Class<?> type) {
            if (RENDER_METHOD == null) {
                return false;
            }
            try {
                return type.getMethod(RENDER_METHOD, PARAMETERS).getDeclaringClass() == Particle.class;
            } catch (ReflectiveOperationException | SecurityException e) {
                return false;
            }
        }
    };

    private ParticleRenderers() {
    }

    public static boolean usesBaseRenderer(final Class<? extends Particle> type) {
        return BASE_RENDERER.get(type);
    }

    private static String findRenderMethod() {
        // Resolve from the loaded base class's signature: no hard-coded MCP/SRG name.
        String name = null;
        try {
            for (final Method method : Particle.class.getDeclaredMethods()) {
                if (method.getReturnType() == void.class
                    && Arrays.equals(method.getParameterTypes(), PARAMETERS)) {
                    if (name != null) {
                        return null;
                    }
                    name = method.getName();
                }
            }
        } catch (SecurityException e) {
            return null;
        }
        return name;
    }
}
