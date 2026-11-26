package github.kasuminova.stellarcore.common.util;

import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutableResourcePackBindingsTest {
    final MutableResourcePackBindings bindings = new MutableResourcePackBindings();
    final ResourceLocation location = new ResourceLocation("covered", "missing.json");

    @Test
    void skipsPerPathMissKeysOnlyWhenAllMutableBindingsCoverTheNamespace() throws Exception {
        assertFalse(bindings.canDiscover(location, true));
        assertEquals(0, missKeys().size());
        final Pack first = new Pack();
        record(first, "covered");
        assertFalse(bindings.canDiscover(location, true));
        assertEquals(0, first.probes);
        assertEquals(0, missKeys().size());
        assertFalse(bindings.canDiscover(location, false));
        assertEquals(1, missKeys().size());

        record(first, "other"); // A repeated physical pack still has an unbound binding.
        assertFalse(bindings.canDiscover(location, true));
        assertFalse(bindings.canDiscover(location, true));
        assertEquals(1, first.probes);
        assertEquals(1, missKeys().size());
        final Pack second = new Pack();
        second.exists = true;
        record(second, "unrelated"); // Could supply an undeclared file in the previous namespace.
        assertTrue(bindings.canDiscover(location, true));
        assertEquals(0, missKeys().size());
        final MutableResourcePackBindings.RefreshPlan plan = bindings.discoverResource(null, location);
        assertTrue(plan.namespaces().contains("covered"));
        bindings.commit(plan);
        assertFalse(bindings.canDiscover(location, true));
        bindings.beginFullReload();
        assertEquals(0, missKeys().size());
        assertFalse(bindings.canDiscover(location, true));
    }

    private void record(final Pack pack, final String namespace) {
        bindings.commit(bindings.prepareRecordPack(pack, Collections.singleton(namespace)));
    }

    private Set<?> missKeys() throws Exception {
        final Field snapshotField = MutableResourcePackBindings.class.getDeclaredField("snapshot");
        snapshotField.setAccessible(true);
        final Object snapshot = snapshotField.get(bindings);
        final Field misses = snapshot.getClass().getDeclaredField("unclaimed");
        misses.setAccessible(true);
        return (Set<?>) misses.get(snapshot);
    }

    private static final class Pack extends FolderResourcePack {
        boolean exists;
        int probes;
        Pack() { super(new File(".")); }
        @Override public boolean resourceExists(final ResourceLocation location) {
            probes++;
            return exists;
        }
    }
}
