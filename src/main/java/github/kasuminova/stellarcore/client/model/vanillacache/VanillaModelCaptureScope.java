package github.kasuminova.stellarcore.client.model.vanillacache;

import net.minecraftforge.client.model.ICustomModelLoader;

/**
 * Capture belongs to the registry call frame, including exceptional exits.
 */
public final class VanillaModelCaptureScope {

    private static Class<?> VL;

    private VanillaModelCaptureScope() {
    }

    public static boolean begin(final ICustomModelLoader loader) {
        if (loader.getClass() != getVanillaLoaderClass()) {
            return false;
        }
        ModelCapture.begin();
        return true;
    }

    public static void end(final boolean opened) {
        if (opened) {
            ModelCapture.end();
        }
    }

    public static Class<?> getVanillaLoaderClass() {
        if (VL == null) {
            try {
                return VL = Class.forName("net.minecraftforge.client.model.ModelLoader$VanillaLoader");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return VL;
    }
}
