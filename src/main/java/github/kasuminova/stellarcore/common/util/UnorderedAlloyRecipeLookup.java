package github.kasuminova.stellarcore.common.util;

import com.enderio.core.common.util.NNList;
import crazypants.enderio.base.recipe.lookup.TriItemLookup;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

/**
 * Alloy-only multiset index. The original one-item tree retains its iteration and recipe priority semantics.
 * Two-item queries include prefixes of three-input recipes, just as the original permutation tree does.
 */
public final class UnorderedAlloyRecipeLookup<R> extends TriItemLookup<R> {
    private final Reference2IntOpenHashMap<Item> ids = new Reference2IntOpenHashMap<>();
    private final Long2ObjectOpenHashMap<NNList<R>> pairs = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<NNList<R>>> triples = new Long2ObjectOpenHashMap<>();

    public UnorderedAlloyRecipeLookup() {
        ids.defaultReturnValue(-1);
    }

    public void addUnordered(final R recipe, final Item a, final Item b) {
        if (a == Items.AIR) {
            super.addRecipe(recipe, b);
        } else if (b == Items.AIR) {
            super.addRecipe(recipe, a);
        } else {
            super.addRecipe(recipe, a);
            if (a != b) {
                super.addRecipe(recipe, b);
            }
            addPair(recipe, id(a), id(b));
        }
    }

    public void addUnordered(final R recipe, final Item a, final Item b, final Item c) {
        if (a == Items.AIR) {
            addUnordered(recipe, b, c);
            return;
        }
        if (b == Items.AIR) {
            addUnordered(recipe, a, c);
            return;
        }
        if (c == Items.AIR) {
            addUnordered(recipe, a, b);
            return;
        }
        super.addRecipe(recipe, a);
        if (b != a) {
            super.addRecipe(recipe, b);
        }
        if (c != a && c != b) {
            super.addRecipe(recipe, c);
        }
        int x = id(a);
        int y = id(b);
        int z = id(c);
        if (x > y) { final int swap = x; x = y; y = swap; }
        if (y > z) { final int swap = y; y = z; z = swap; }
        if (x > y) { final int swap = x; x = y; y = swap; }
        addPair(recipe, x, y);
        if (z != y) {
            addPair(recipe, x, z);
        }
        if (x != y) {
            addPair(recipe, y, z);
        }
        final long key = pairKey(x, y);
        Int2ObjectOpenHashMap<NNList<R>> suffixes = triples.get(key);
        if (suffixes == null) {
            suffixes = new Int2ObjectOpenHashMap<>();
            triples.put(key, suffixes);
        }
        NNList<R> recipes = suffixes.get(z);
        if (recipes == null) {
            recipes = new NNList<>();
            suffixes.put(z, recipes);
        }
        append(recipes, recipe);
    }

    @Override
    public NNList<R> getRecipes(final Item a, final Item b) {
        final int x = ids.getInt(a);
        final int y = ids.getInt(b);
        if (x < 0 || y < 0) {
            return NNList.emptyList();
        }
        final NNList<R> recipes = pairs.get(pairKey(x, y));
        return recipes == null ? NNList.emptyList() : recipes;
    }

    @Override
    public NNList<R> getRecipes(final Item a, final Item b, final Item c) {
        int x = ids.getInt(a);
        int y = ids.getInt(b);
        int z = ids.getInt(c);
        if (x < 0 || y < 0 || z < 0) {
            return NNList.emptyList();
        }
        if (x > y) { final int swap = x; x = y; y = swap; }
        if (y > z) { final int swap = y; y = z; z = swap; }
        if (x > y) { final int swap = x; x = y; y = swap; }
        final Int2ObjectOpenHashMap<NNList<R>> suffixes = triples.get(pairKey(x, y));
        final NNList<R> recipes = suffixes == null ? null : suffixes.get(z);
        return recipes == null ? NNList.emptyList() : recipes;
    }

    private int id(final Item item) {
        int id = ids.getInt(item);
        if (id < 0) {
            id = ids.size();
            ids.put(item, id);
        }
        return id;
    }

    private void addPair(final R recipe, final int a, final int b) {
        final long key = pairKey(a, b);
        NNList<R> recipes = pairs.get(key);
        if (recipes == null) {
            recipes = new NNList<>();
            pairs.put(key, recipes);
        }
        append(recipes, recipe);
    }

    private static long pairKey(final int a, final int b) {
        return (long) Math.min(a, b) << 32 | (Math.max(a, b) & 0xFFFFFFFFL);
    }

    private static <R> void append(final NNList<R> recipes, final R recipe) {
        if ((recipes.isEmpty() || recipes.get(recipes.size() - 1) != recipe) && !recipes.contains(recipe)) {
            recipes.add(recipe);
        }
    }
}
