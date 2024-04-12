package github.kasuminova.stellarcore.common.util;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import java.util.Base64;
import java.util.Objects;

@Desugar
public record HashedItemStack(ItemStack stack, int stackHashCode, boolean hasTag) {

    public static HashedItemStack ofTag(final ItemStack stack) {
        ItemStack copied = stack.copy();
        NBTTagCompound tag = copied.getTagCompound();
        boolean hasTag = tag == null || tag.isEmpty();
        int hash;
        if (hasTag) {
            hash = Objects.hash(copied.getItem(), copied.getItemDamage(), tag);
        } else {
            hash = Objects.hash(copied.getItem(), copied.getItemDamage());
        }
        return new HashedItemStack(copied, hash, hasTag);
    }

    public static HashedItemStack ofTagUnsafe(final ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        boolean hasTag = tag == null || tag.isEmpty();
        int hash;
        if (hasTag) {
            hash = Objects.hash(stack.getItem(), stack.getItemDamage(), tag);
        } else {
            hash = Objects.hash(stack.getItem(), stack.getItemDamage());
        }
        return new HashedItemStack(stack, hash, hasTag);
    }

    public static HashedItemStack ofMeta(final ItemStack stack) {
        ItemStack copied = stack.copy();
        return new HashedItemStack(copied, Objects.hash(copied.getItem(), copied.getMetadata()), false);
    }

    public static HashedItemStack ofMetaUnsafe(final ItemStack stack) {
        return new HashedItemStack(stack, Objects.hash(stack.getItem(), stack.getMetadata()), false);
    }

    public static String stackToString(final ItemStack stack) {
        String stackTagStr = null;
        String registryName = Objects.requireNonNull(stack.getItem().getRegistryName()).toString();
        if (stack.getTagCompound() != null) {
            stackTagStr = stack.getTagCompound().toString();
        }
        return strToBase64(registryName) + "_" + stack.getItemDamage() + (stackTagStr == null ? "" : "_" + strToBase64(stackTagStr));
    }

    public static String strToBase64(final String str) {
        return Base64.getEncoder().encodeToString(str.getBytes());
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof HashedItemStack hashedItemStack) {
            if (hasTag && !hashedItemStack.hasTag) {
                return false;
            }
            return stackEqualsNonNBT(stack, hashedItemStack.stack) && (!hasTag || ItemStack.areItemStackTagsEqual(stack, hashedItemStack.stack));
        }
        return false;
    }

    public static boolean stackEqualsNonNBT(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (stack.isEmpty() && other.isEmpty()) {
            return true;
        }
        if (stack.isEmpty() || other.isEmpty()) {
            return false;
        }
        Item sItem = stack.getItem();
        Item oItem = other.getItem();
        if (sItem.getHasSubtypes() || oItem.getHasSubtypes()) {
            return sItem.equals(other.getItem()) &&
                    (stack.getItemDamage() == other.getItemDamage() ||
                            stack.getItemDamage() == OreDictionary.WILDCARD_VALUE ||
                            other.getItemDamage() == OreDictionary.WILDCARD_VALUE);
        } else {
            return sItem.equals(other.getItem());
        }
    }

    @Override
    public int hashCode() {
        return stackHashCode;
    }
}
