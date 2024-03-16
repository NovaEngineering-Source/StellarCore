package github.kasuminova.stellarcore.mixin.util;

import net.minecraft.item.ItemStack;

public interface CachedItemConduit {

    ItemStack getCachedStack();

    void setCachedStack(final ItemStack cachedStack);

}
