package github.kasuminova.stellarcore.mixin.enderio;

import crazypants.enderio.base.recipe.IRecipeInput;
import crazypants.enderio.base.recipe.MachineRecipeInput;
import crazypants.enderio.base.recipe.Recipe;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.HashedItemStack;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(value = Recipe.class, remap = false)
public class MixinRecipe {

    @Shadow
    @Final
    @Nonnull
    private IRecipeInput[] inputs;

    @Unique
    private final Map<HashedItemStack, Boolean> stellar_core$anyInputItemCache = new WeakHashMap<>();

    @Unique
    private final Map<Fluid, Boolean> stellar_core$anyInputFluidCache = new WeakHashMap<>();

    @Inject(method = "isAnyInput", at = @At("HEAD"), cancellable = true)
    private void injectIsAnyInput(final MachineRecipeInput realInput, final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.PERFORMANCE.enderIO.recipe) {
            return;
        }

        ItemStack item = realInput.item;
        FluidStack fluid = realInput.fluid;

        HashedItemStack hashedStack = null;

        if (!item.isEmpty()) {
            hashedStack = HashedItemStack.ofTagUnsafe(item);
            Boolean ret = stellar_core$anyInputItemCache.get(hashedStack);
            if (ret != null) {
                cir.setReturnValue(ret);
                return;
            }
        }
        if (fluid != null) {
            Boolean ret = stellar_core$anyInputFluidCache.get(fluid.getFluid());
            if (ret != null) {
                cir.setReturnValue(ret);
                return;
            }
        }

        for (IRecipeInput recipeInput : inputs) {
            if (recipeInput != null && ((recipeInput.isInput(item)) || recipeInput.isInput(realInput.fluid))) {
                stellar_core$storeCache(hashedStack, fluid, true);
                cir.setReturnValue(true);
                return;
            }
        }

        stellar_core$storeCache(hashedStack, fluid, false);
        cir.setReturnValue(false);
    }

    @Unique
    private void stellar_core$storeCache(final HashedItemStack hashedItemStack, final FluidStack fluid, final boolean value) {
        if (hashedItemStack != null) {
            stellar_core$anyInputItemCache.put(hashedItemStack, value);
        }
        if (fluid != null) {
            stellar_core$anyInputFluidCache.put(fluid.getFluid(), value);
        }
    }

}
