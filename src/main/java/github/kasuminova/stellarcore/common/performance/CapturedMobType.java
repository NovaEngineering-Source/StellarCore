package github.kasuminova.stellarcore.common.performance;

import crazypants.enderio.util.CapturedMob;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/** Read-only equivalent of CapturedMob.create(stack).isSameType(entity), without copying entity NBT. */
public final class CapturedMobType {
    private static final ResourceLocation PIG = new ResourceLocation("minecraft", "pig");

    private CapturedMobType() {
    }

    public static boolean matches(final ItemStack stack, final Entity entity) {
        // Keep EnderIO's validity check, including non-soul items carrying legacy soul NBT.
        if (!CapturedMob.containsSoul(stack)) return false;
        final ResourceLocation type = readType(stack.getTagCompound());
        return entity != null && type.equals(EntityList.getKey(entity));
    }

    static ResourceLocation readType(final NBTTagCompound tag) {
        String id = null;
        if (tag.hasKey("entityId")) {
            // Presence, rather than NBT type, is deliberate: a mistyped/empty ID suppresses fallback.
            id = tag.getString("entityId");
        } else {
            final NBTTagCompound entity;
            if (tag.hasKey("entity")) entity = tag.getCompoundTag("entity");
            else if (tag.hasKey("EntityTag")) entity = tag.getCompoundTag("EntityTag");
            else entity = null;
            if (entity != null && entity.hasKey("id")) id = entity.getString("id");
        }
        // Match CapturedMob's legacy pig fallback and ResourceLocation's normalization/validation.
        return id == null || id.isEmpty() ? PIG : new ResourceLocation(id);
    }
}
