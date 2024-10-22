package github.kasuminova.stellarcore.mixin.nco;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.HashedItemStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import nc.recipe.AbstractRecipeHandler;
import nc.recipe.BasicRecipe;
import nc.recipe.BasicRecipeHandler;
import nc.recipe.IngredientSorption;
import nc.recipe.ingredient.IFluidIngredient;
import nc.recipe.ingredient.IItemIngredient;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(value = BasicRecipeHandler.class, remap = false)
public abstract class MixinBasicRecipeHandler extends AbstractRecipeHandler<BasicRecipe> {

    @Shadow
    @Final
    public boolean isShapeless;

    @Unique
    private volatile Map<HashedItemStack, List<BasicRecipe>> stellar_core$itemRecipeCache = null;

    @Unique
    private volatile Map<FluidStack, List<BasicRecipe>> stellar_core$fluidRecipeCache = null;

    @Inject(method = "isValidItemInput(Lnet/minecraft/item/ItemStack;I)Z", at = @At("HEAD"), cancellable = true)
    private void injectIsValidItemInput(final ItemStack stack, final int slot, final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.PERFORMANCE.nuclearCraftOverhauled.basicRecipeImprovements) {
            return;
        }
        cir.setReturnValue(stellar_core$isValidItemInput(stack, slot));
    }

    @Inject(method = "isValidFluidInput(Lnet/minecraftforge/fluids/FluidStack;I)Z", at = @At("HEAD"), cancellable = true)
    private void injectIsValidFluidInput(final FluidStack stack, final int tankNumber, final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.PERFORMANCE.nuclearCraftOverhauled.basicRecipeImprovements) {
            return;
        }
        cir.setReturnValue(stellar_core$isValidFluidInput(stack, tankNumber));
    }

    @Unique
    private boolean stellar_core$isValidItemInput(final ItemStack stack, final int index) {
        final HashedItemStack hashedStack = HashedItemStack.ofMetaUnsafe(stack);
        List<BasicRecipe> cached = stellar_core$getItemRecipeCache().get(hashedStack);
        if (cached != null && cached.size() > index) {
            final BasicRecipe recipe = cached.get(index);
            if (recipe != null && stellar_core$isItemRecipeValid(stack, index, recipe)) {
                return true;
            }
        }

        for (BasicRecipe recipe : recipeList) {
            if (stellar_core$isItemRecipeValid(stack, index, recipe)) {
                if (cached == null) {
                    cached = stellar_core$getItemRecipeCache().computeIfAbsent(hashedStack.copy(), k -> new ObjectArrayList<>());
                }
                while (cached.size() <= index) {
                    cached.add(null);
                }
                cached.set(index, recipe);
                return true;
            }
        }

        return false;
    }

    @Unique
    private boolean stellar_core$isItemRecipeValid(final ItemStack stack, final int index, final BasicRecipe recipe) {
        if (isShapeless) {
            for (final IItemIngredient input : recipe.getItemIngredients()) {
                if (input.match(stack, IngredientSorption.NEUTRAL).matches()) {
                    return true;
                }
            }
        } else {
            return recipe.getItemIngredients().get(index).match(stack, IngredientSorption.NEUTRAL).matches();
        }
        return false;
    }

    @Unique
    private boolean stellar_core$isValidFluidInput(FluidStack stack, int index) {
        List<BasicRecipe> cached = stellar_core$getFluidRecipeCache().get(stack);
        if (cached != null && cached.size() > index) {
            final BasicRecipe recipe = cached.get(index);
            if (recipe != null && stellar_core$isFluidRecipeValid(stack, index, recipe)) {
                return true;
            }
        }

        for (BasicRecipe recipe : recipeList) {
            if (stellar_core$isFluidRecipeValid(stack, index, recipe)) {
                if (cached == null) {
                    cached = stellar_core$getFluidRecipeCache().computeIfAbsent(stack.copy(), k -> new ObjectArrayList<>());
                }
                while (cached.size() <= index) {
                    cached.add(null);
                }
                cached.set(index, recipe);
                return true;
            }
        }

        return false;
    }

    @Unique
    private boolean stellar_core$isFluidRecipeValid(final FluidStack stack, final int index, final BasicRecipe recipe) {
        if (isShapeless) {
            for (final IFluidIngredient input : recipe.getFluidIngredients()) {
                if (input.match(stack, IngredientSorption.NEUTRAL).matches()) {
                    return true;
                }
            }
        } else {
            return recipe.getFluidIngredients().get(index).match(stack, IngredientSorption.NEUTRAL).matches();
        }
        return false;
    }

    @Unique
    private Map<HashedItemStack, List<BasicRecipe>> stellar_core$getItemRecipeCache() {
        if (stellar_core$itemRecipeCache == null) {
            synchronized (this) {
                if (stellar_core$itemRecipeCache == null) {
                    stellar_core$itemRecipeCache = Collections.synchronizedMap(new WeakHashMap<>());
                }
            }
        }
        return stellar_core$itemRecipeCache;
    }

    @Unique
    private Map<FluidStack, List<BasicRecipe>> stellar_core$getFluidRecipeCache() {
        if (stellar_core$fluidRecipeCache == null) {
            synchronized (this) {
                if (stellar_core$fluidRecipeCache == null) {
                    stellar_core$fluidRecipeCache = Collections.synchronizedMap(new WeakHashMap<>());
                }
            }
        }
        return stellar_core$fluidRecipeCache;
    }

}
