package github.kasuminova.stellarcore.common.performance;

import crazypants.enderio.util.CapturedMob;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CapturedMobTypeTest {
    @Test
    void idSelectionAndMalformedTagFallbackMatchRealEnderIoWithoutMutatingNbt() {
        for (final String container : Arrays.asList("entity", "EntityTag")) {
            for (final String id : Arrays.asList("", "pig", "minecraft:pig", "minecraft:zombie", "custom:mob", "UPPER:Case", ":", "odd path")) {
                final NBTTagCompound entity = new NBTTagCompound();
                entity.setString("id", id);
                entity.setIntArray("payload", new int[]{1, 2, 3, 4});
                final NBTTagCompound tag = new NBTTagCompound();
                tag.setTag(container, entity);
                compare(tag);
                tag.setString("entityId", "minecraft:cow");
                compare(tag);
                tag.setString("entityId", "");
                compare(tag);
                tag.setInteger("entityId", 123);
                compare(tag);
            }
        }
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("entity", 123);
        final NBTTagCompound legacy = new NBTTagCompound();
        legacy.setString("id", "minecraft:zombie");
        tag.setTag("EntityTag", legacy);
        compare(tag); // Presence of the mistyped modern key still takes precedence.
        tag.removeTag("entity");
        compare(tag);
        tag.setInteger("EntityTag", 123);
        compare(tag);
        tag.removeTag("EntityTag");
        assertFalse(CapturedMob.containsSoul(tag));
    }

    private static void compare(final NBTTagCompound tag) {
        final NBTTagCompound before = tag.copy();
        final CapturedMob original;
        try {
            original = CapturedMob.create(tag);
        } catch (final RuntimeException exception) {
            assertThrows(exception.getClass(), () -> CapturedMobType.readType(tag));
            assertEquals(before, tag);
            return;
        }
        assertNotNull(original);
        assertEquals(original.getEntityName(), CapturedMobType.readType(tag));
        assertEquals(before, tag);
    }
}
