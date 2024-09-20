package github.kasuminova.stellarcore.common.util;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class StackUtils {

    public static ItemStack loadStackWithNonCap(final NBTTagCompound stackTag) {
        Item item = stackTag.hasKey("id", 8) ? Item.getByNameOrId(stackTag.getString("id")) : Items.AIR;
        if (item == null) {
            return ItemStack.EMPTY;
        }

        int stackSize = stackTag.getByte("Count");
        int itemDamage = Math.max(0, stackTag.getShort("Damage"));
        ItemStack stack = new ItemStack(item, stackSize, itemDamage, null);
        if (stackTag.hasKey("tag", 10)) {
            stack.setTagCompound(stackTag.getCompoundTag("tag"));
        }
        return stack;
    }

}
