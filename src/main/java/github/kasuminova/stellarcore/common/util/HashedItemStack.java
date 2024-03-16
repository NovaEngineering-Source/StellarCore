package github.kasuminova.stellarcore.common.util;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.item.ItemStack;

import java.util.Base64;
import java.util.Objects;

@Desugar
public record HashedItemStack(ItemStack stack, int stackHashCode) {
    public static HashedItemStack of(final ItemStack stack) {
        ItemStack copied = stack.copy();
        return new HashedItemStack(copied, Objects.hash(copied.getItem().getRegistryName(), copied.getItemDamage(), copied.getTagCompound()));
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
            return ItemStack.areItemStacksEqual(stack, hashedItemStack.stack);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return stackHashCode;
    }
}
