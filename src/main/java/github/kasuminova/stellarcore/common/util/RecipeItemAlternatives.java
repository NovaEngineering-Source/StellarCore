package github.kasuminova.stellarcore.common.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

public final class RecipeItemAlternatives {

    private RecipeItemAlternatives() {
    }

    /**
     * Retains first-occurrence order without repeating scans in each Cartesian-product branch.
     */
    public static List<Item> uniqueItems(final List<ItemStack> alternatives) {
        final int size = alternatives.size();
        if (size == 0) {
            return Collections.emptyList();
        }
        if (size == 1) {
            return Collections.singletonList(alternatives.get(0).getItem());
        }
        final ObjectArrayList<Item> items = new ObjectArrayList<>(size);
        if (size > 16) {
            final ReferenceOpenHashSet<Item> seen = new ReferenceOpenHashSet<>();
            for (final ItemStack stack : alternatives) {
                final Item item = stack.getItem();
                if (seen.add(item)) {
                    items.add(item);
                }
            }
        } else {
            outer:
            for (final ItemStack stack : alternatives) {
                final Item item = stack.getItem();
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i) == item) {
                        continue outer;
                    }
                }
                items.add(item);
            }
        }
        return items;
    }
}
