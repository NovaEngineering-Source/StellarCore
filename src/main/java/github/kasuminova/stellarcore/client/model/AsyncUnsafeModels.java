package github.kasuminova.stellarcore.client.model;

import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingIdentityHashSet;
import net.minecraftforge.client.model.IModel;

import java.util.Set;

public final class AsyncUnsafeModels {

    private static final Set<IModel> MODELS = new NonBlockingIdentityHashSet<>();

    private static final Object BAKE_LOCK = new Object();

    private AsyncUnsafeModels() {
    }

    public static void register(final IModel model) {
        MODELS.add(model);
    }

    public static boolean contains(final IModel model) {
        return MODELS.contains(model);
    }

    public static Object bakeLock() {
        return BAKE_LOCK;
    }

    public static void clear() {
        MODELS.clear();
    }

}
