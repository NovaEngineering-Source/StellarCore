package github.kasuminova.stellarcore.mixin.util;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.smeltery.AlloyRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TinkerRegistryUtils {
    private static final Map<Fluid, List<AlloyRecipe>> ALLOY_RECIPE_PREFIX_MAP = new HashMap<>();

    public static boolean stellar_core$alloyRecipeDirty = false;

    public static Map<Fluid, List<AlloyRecipe>> getAlloyRecipePrefixMap() {
        if (stellar_core$alloyRecipeDirty) {
            stellar_core$alloyRecipeDirty = false;
            ALLOY_RECIPE_PREFIX_MAP.clear();
            TinkerRegistry.getAlloys().forEach(recipe -> {
                if (!recipe.isValid()) {
                    return;
                }
                FluidStack stack = recipe.getFluids().get(0);
                ALLOY_RECIPE_PREFIX_MAP.computeIfAbsent(stack.getFluid(), k -> new ArrayList<>()).add(recipe);
            });
        }
        return ALLOY_RECIPE_PREFIX_MAP;
    }

}
