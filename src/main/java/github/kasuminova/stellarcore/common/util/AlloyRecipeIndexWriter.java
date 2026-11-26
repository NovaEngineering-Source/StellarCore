package github.kasuminova.stellarcore.common.util;

import crazypants.enderio.base.recipe.lookup.IRecipeNode;
import crazypants.enderio.base.recipe.lookup.ItemRecipeLeafNode;
import crazypants.enderio.base.recipe.lookup.ItemRecipeNode;
import crazypants.enderio.base.recipe.lookup.TriItemLookup;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

import java.util.function.Supplier;

/**
 * Reuses prefixes only within the contiguous, callback-free insertion of one recipe's item permutations.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class AlloyRecipeIndexWriter<R> {
    private static final Supplier<ItemRecipeNode> NODE = ItemRecipeNode::new;
    private static final Supplier<ItemRecipeLeafNode> LEAF = ItemRecipeLeafNode::new;
    private final TriItemLookup<R> lookup;
    private final R recipe;
    private final ItemRecipeNode<R, ItemRecipeNode<R, ItemRecipeLeafNode<R>>> root;
    private final Object[] owners;
    private final IRecipeNode<?, ?, ?>[] children;
    private final int[] keys;
    private final int mask;

    public AlloyRecipeIndexWriter(final TriItemLookup<R> lookup, final R recipe) {
        this(lookup, recipe, 128);
    }

    public AlloyRecipeIndexWriter(final TriItemLookup<R> lookup, final R recipe, final int distinctItems) {
        this.lookup = lookup;
        this.recipe = recipe;
        root = lookup.getClass() == TriItemLookup.class && lookup instanceof TriItemLookupRootAccess<?>
            ? (ItemRecipeNode) ((TriItemLookupRootAccess<?>) lookup).stellar_core$getRecipeRoot() : null;
        int capacity = 16;
        while (capacity < 256 && capacity < (long) distinctItems * 2) {
            capacity <<= 1;
        }
        mask = capacity - 1;
        owners = root == null ? null : new Object[capacity];
        children = root == null ? null : new IRecipeNode[capacity];
        keys = root == null ? null : new int[capacity];
    }

    public void add(final Item first) {
        if (root == null) {
            lookup.addRecipe(recipe, first);
        } else if (first == Items.AIR) {
            throw new RuntimeException("no items to store recipe for");
        } else {
            next(root, first, (Supplier) NODE);
        }
    }

    public void add(final Item first, final Item second) {
        if (root == null) {
            lookup.addRecipe(recipe, first, second);
        } else if (first == Items.AIR) {
            add(second);
        } else if (second == Items.AIR) {
            add(first);
        } else {
            final ItemRecipeNode<R, ItemRecipeLeafNode<R>> middle = next(root, first, (Supplier) NODE);
            next(middle, second, (Supplier) LEAF);
        }
    }

    public void add(final Item first, final Item second, final Item third) {
        if (root == null) {
            lookup.addRecipe(recipe, first, second, third);
        } else if (first == Items.AIR) {
            add(second, third);
        } else if (second == Items.AIR) {
            add(first, third);
        } else if (third == Items.AIR) {
            add(first, second);
        } else {
            final ItemRecipeNode<R, ItemRecipeLeafNode<R>> middle = next(root, first, (Supplier) NODE);
            final ItemRecipeLeafNode<R> leaf = next(middle, second, (Supplier) LEAF);
            leaf.addRecipe(recipe, third);
        }
    }

    private <C extends IRecipeNode<?, ?, ?>> C next(final ItemRecipeNode<R, C> owner, final Item key,
                                                    final Supplier<C> factory) {
        final int id = Item.getIdFromItem(key);
        final int hash = System.identityHashCode(owner) ^ id * 0x9E3779B9;
        final int slot = (hash ^ hash >>> 16) & mask;
        if (owners[slot] == owner && keys[slot] == id) {
            return (C) children[slot];
        }
        final C child = owner.makeNext(recipe, key, factory);
        owners[slot] = owner;
        keys[slot] = id;
        children[slot] = child;
        return child;
    }
}
