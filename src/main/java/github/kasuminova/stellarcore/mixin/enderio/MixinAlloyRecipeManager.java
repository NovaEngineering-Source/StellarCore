package github.kasuminova.stellarcore.mixin.enderio;

import crazypants.enderio.base.recipe.IManyToOneRecipe;
import crazypants.enderio.base.recipe.alloysmelter.AlloyRecipeManager;
import crazypants.enderio.base.recipe.lookup.TriItemLookup;
import github.kasuminova.stellarcore.common.util.AlloyRecipeIndexWriter;
import github.kasuminova.stellarcore.common.util.RecipeItemAlternatives;
import github.kasuminova.stellarcore.common.util.UnorderedAlloyRecipeLookup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = AlloyRecipeManager.class, remap = false)
public abstract class MixinAlloyRecipeManager {
    @Redirect(method = {"<init>", "rebuild"}, at = @At(value = "NEW",
        target = "crazypants/enderio/base/recipe/lookup/TriItemLookup"), require = 2)
    private TriItemLookup<IManyToOneRecipe> stellar_core$unorderedLookup() {
        return new UnorderedAlloyRecipeLookup<>();
    }

    /**
     * @author circulation
     * @reason Index item identities once instead of repeating metadata alternatives and permutations.
     */
    @Overwrite
    private static void addRecipeToLookup(final TriItemLookup<IManyToOneRecipe> lookup,
                                          final IManyToOneRecipe recipe) {
        final List<? extends List<ItemStack>> alternatives = recipe.getInputStackAlternatives();
        if (alternatives.isEmpty() || alternatives.size() > 3) return;
        final List<Item> first = RecipeItemAlternatives.uniqueItems(alternatives.get(0));
        if (first.isEmpty()) return;
        if (alternatives.size() == 1) {
            for (int i = 0; i < first.size(); i++) lookup.addRecipe(recipe, first.get(i));
            return;
        }
        final List<Item> second = RecipeItemAlternatives.uniqueItems(alternatives.get(1));
        if (second.isEmpty()) return;
        final UnorderedAlloyRecipeLookup<IManyToOneRecipe> unordered = lookup instanceof UnorderedAlloyRecipeLookup<?>
            ? (UnorderedAlloyRecipeLookup<IManyToOneRecipe>) lookup : null;
        if (alternatives.size() == 2) {
            final AlloyRecipeIndexWriter<IManyToOneRecipe> writer = unordered == null
                ? new AlloyRecipeIndexWriter<>(lookup, recipe, first.size() + second.size()) : null;
            for (int i = 0; i < first.size(); i++) {
                final Item a = first.get(i);
                for (int j = 0; j < second.size(); j++) {
                    final Item b = second.get(j);
                    if (unordered != null) {
                        unordered.addUnordered(recipe, a, b);
                        continue;
                    }
                    writer.add(a, b);
                    if (a != b) writer.add(b, a);
                }
            }
            return;
        }
        final List<Item> third = RecipeItemAlternatives.uniqueItems(alternatives.get(2));
        final AlloyRecipeIndexWriter<IManyToOneRecipe> writer = unordered == null
            ? new AlloyRecipeIndexWriter<>(lookup, recipe, first.size() + second.size() + third.size()) : null;
        for (int i = 0; i < first.size(); i++) {
            final Item a = first.get(i);
            for (int j = 0; j < second.size(); j++) {
                final Item b = second.get(j);
                for (int k = 0; k < third.size(); k++) {
                    final Item c = third.get(k);
                    if (unordered != null) {
                        unordered.addUnordered(recipe, a, b, c);
                        continue;
                    }
                    writer.add(a, b, c);
                    if (b != c) writer.add(a, c, b);
                    if (a != b) writer.add(b, a, c);
                    if (a != b && a != c) writer.add(b, c, a);
                    if (c != a && c != b) writer.add(c, a, b);
                    if (a != b && a != c && b != c) writer.add(c, b, a);
                }
            }
        }
    }
}
