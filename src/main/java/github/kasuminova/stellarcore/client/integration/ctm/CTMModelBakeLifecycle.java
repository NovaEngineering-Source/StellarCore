package github.kasuminova.stellarcore.client.integration.ctm;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/**
 * The reload return point follows both the loader workers and ModelBakeEvent handlers.
 */
public final class CTMModelBakeLifecycle {
    private static volatile ReferenceOpenHashSet<Model> pending;

    private CTMModelBakeLifecycle() {
    }

    public static synchronized void begin() {
        pending = new ReferenceOpenHashSet<>();
    }

    public static synchronized void track(final Model model) {
        if (pending != null) {
            pending.add(model);
        }
    }

    public static boolean isLoading() {
        return pending != null;
    }

    public static synchronized void abort() {
        // A failed parallel stream can still have workers leaving their call
        // frames. Release the roots without copying maps they may still update.
        pending = null;
    }

    public static void complete() {
        final ReferenceOpenHashSet<Model> models;
        synchronized (CTMModelBakeLifecycle.class) {
            models = pending;
            pending = null;
        }
        if (models != null) {
            for (final Model model : models) {
                model.stellar_core$compactTextureMaps();
            }
        }
    }

    public interface Model {
        void stellar_core$compactTextureMaps();
    }
}
