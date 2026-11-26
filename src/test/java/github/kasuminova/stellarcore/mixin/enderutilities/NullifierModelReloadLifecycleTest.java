package github.kasuminova.stellarcore.mixin.enderutilities;

import org.junit.jupiter.api.Test;
import net.minecraftforge.client.event.ModelBakeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NullifierModelReloadLifecycleTest {
    private static void set(final Class<?> mixin, final String name, final Object value) throws Exception {
        field(mixin, name).set(null, value);
    }

    private static Field field(final Class<?> mixin, final String name) throws Exception {
        final Field field = mixin.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @Test
    void reloadDropsPreviousFallbackAndBakedCachesWithoutDestroyingTheirSharedMaps() throws Exception {
        final Class<?> mixin = MixinNullifierModelReloadLifecycle.class;
        final Map<Object, Object> previousStateModels = new HashMap<>();
        final Map<Object, Object> previousLocations = new HashMap<>();
        final Map<Object, Object> previousShapers = new HashMap<>();
        final Map<Object, Object> nullifiers = new HashMap<>();
        final Map<Object, Object> items = new HashMap<>();
        previousStateModels.put(new Object(), new Object());
        previousLocations.put(new Object(), new Object());
        previousShapers.put(new Object(), new Object());
        nullifiers.put(new Object(), new Object());
        items.put(new Object(), new Object());
        set(mixin, "STATE_MODELS", previousStateModels);
        set(mixin, "LOCATIONS", previousLocations);
        set(mixin, "SHAPERS", previousShapers);
        set(mixin, "NULLIFIER_MODEL_CACHE", nullifiers);
        set(mixin, "ITEM_MODEL_CACHE", items);
        final Method callback = mixin.getDeclaredMethod("onModelbakeEvent", ModelBakeEvent.class);
        callback.setAccessible(true);
        callback.invoke(null, new Object[]{null});
        assertNull(field(mixin, "MODEL_LOADER").get(null));
        assertNull(field(mixin, "STATE_MODELS").get(null));
        assertNull(field(mixin, "LOCATIONS").get(null));
        assertNull(field(mixin, "SHAPERS").get(null));
        assertEquals(1, previousStateModels.size());
        assertEquals(1, previousLocations.size());
        assertEquals(1, previousShapers.size());
        assertTrue(nullifiers.isEmpty());
        assertTrue(items.isEmpty());
    }
}
