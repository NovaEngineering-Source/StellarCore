package github.kasuminova.stellarcore.common.util;

import crazypants.enderio.base.recipe.lookup.ItemRecipeLeafNode;
import crazypants.enderio.base.recipe.lookup.ItemRecipeNode;

public interface TriItemLookupRootAccess<R> {
    ItemRecipeNode<R, ItemRecipeNode<R, ItemRecipeLeafNode<R>>> stellar_core$getRecipeRoot();
}
