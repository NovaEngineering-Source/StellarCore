package github.kasuminova.stellarcore.mixin.tconstruct;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.HashedItemStack;
import github.kasuminova.stellarcore.common.util.HashedStackFluidPair;
import github.kasuminova.stellarcore.mixin.util.TinkerRegistryUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.smeltery.AlloyRecipe;
import slimeknights.tconstruct.library.smeltery.ICastingRecipe;
import slimeknights.tconstruct.library.smeltery.MeltingRecipe;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

@Mixin(TinkerRegistry.class)
public class MixinTinkerRegistry {

    @Unique
    private static final Map<HashedItemStack, Optional<MeltingRecipe>> MELTING_RECIPE_CACHE = new WeakHashMap<>();

    @Unique
    private static final Map<HashedStackFluidPair, Optional<ICastingRecipe>> TABLE_CASTING_RECIPE_CACHE = new WeakHashMap<>();

    @Unique
    private static final Map<HashedStackFluidPair, Optional<ICastingRecipe>> BASIN_CASTING_RECIPE_CACHE = new WeakHashMap<>();

    @Shadow(remap = false)
    private static List<MeltingRecipe> meltingRegistry;

    @Shadow(remap = false)
    private static List<ICastingRecipe> tableCastRegistry;

    @Shadow(remap = false)
    private static List<ICastingRecipe> basinCastRegistry;

    @Inject(method = "getMelting", at = @At("HEAD"), cancellable = true, remap = false)
    private static void injectGetMelting(final ItemStack stack, final CallbackInfoReturnable<MeltingRecipe> cir) {
        if (!StellarCoreConfig.PERFORMANCE.tConstruct.meltingRecipeSearch) {
            return;
        }

        // Check cache...
        HashedItemStack hashed = HashedItemStack.ofTagUnsafe(stack);
        Optional<MeltingRecipe> result = MELTING_RECIPE_CACHE.get(hashed);
        if (result != null) {
            cir.setReturnValue(result.orElse(null));
            return;
        }

        // Original search...
        for (final MeltingRecipe recipe : meltingRegistry) {
            if (recipe.matches(stack)) {
                // Save result...
                MELTING_RECIPE_CACHE.put(hashed.copy(), Optional.of(recipe));
                cir.setReturnValue(recipe);
                return;
            }
        }

        // Save result...
        MELTING_RECIPE_CACHE.put(hashed.copy(), Optional.empty());
        cir.setReturnValue(null);
    }

    @Inject(method = "getTableCasting", at = @At("HEAD"), cancellable = true, remap = false)
    private static void injectGetTableCasting(final ItemStack cast, final Fluid fluid, final CallbackInfoReturnable<ICastingRecipe> cir) {
        if (!StellarCoreConfig.PERFORMANCE.tConstruct.tableCastingSearch) {
            return;
        }

        // Check cache...
        HashedStackFluidPair hashed = new HashedStackFluidPair(HashedItemStack.ofTagUnsafe(cast), fluid);
        Optional<ICastingRecipe> result = TABLE_CASTING_RECIPE_CACHE.get(hashed);
        if (result != null) {
            cir.setReturnValue(result.orElse(null));
            return;
        }

        // Original search...
        for (final ICastingRecipe recipe : tableCastRegistry) {
            if (recipe.matches(cast, fluid)) {
                // Save result...
                TABLE_CASTING_RECIPE_CACHE.put(hashed.copy(), Optional.of(recipe));
                cir.setReturnValue(recipe);
                return;
            }
        }

        // Save result...
        TABLE_CASTING_RECIPE_CACHE.put(hashed.copy(), Optional.empty());
        cir.setReturnValue(null);
    }

    @Inject(method = "getBasinCasting", at = @At("HEAD"), cancellable = true, remap = false)
    private static void injectGetBasinCasting(final ItemStack cast, final Fluid fluid, final CallbackInfoReturnable<ICastingRecipe> cir) {
        if (!StellarCoreConfig.PERFORMANCE.tConstruct.basinCastingSearch) {
            return;
        }

        // Check cache...
        HashedStackFluidPair hashed = new HashedStackFluidPair(HashedItemStack.ofTagUnsafe(cast), fluid);
        Optional<ICastingRecipe> result = BASIN_CASTING_RECIPE_CACHE.get(hashed);
        if (result != null) {
            cir.setReturnValue(result.orElse(null));
            return;
        }

        // Original search...
        for (final ICastingRecipe recipe : basinCastRegistry) {
            if (recipe.matches(cast, fluid)) {
                // Save result...
                BASIN_CASTING_RECIPE_CACHE.put(hashed.copy(), Optional.of(recipe));
                cir.setReturnValue(recipe);
                return;
            }
        }

        // Save result...
        BASIN_CASTING_RECIPE_CACHE.put(hashed.copy(), Optional.empty());
        cir.setReturnValue(null);
    }

    @Inject(
            method = "registerAlloy(Lslimeknights/tconstruct/library/smeltery/AlloyRecipe;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    remap = false
            ),
            remap = false
    )
    private static void injectRegistryAlloy(final AlloyRecipe recipe, final CallbackInfo ci) {
        TinkerRegistryUtils.stellar_core$alloyRecipeDirty = true;
    }

}
