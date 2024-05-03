package github.kasuminova.stellarcore.mixin.tconstruct;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.TinkerRegistryUtils;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.library.smeltery.AlloyRecipe;
import slimeknights.tconstruct.library.smeltery.SmelteryTank;
import slimeknights.tconstruct.library.utils.FluidUtil;
import slimeknights.tconstruct.smeltery.tileentity.TileSmeltery;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(TileSmeltery.class)
public class MixinTileSmeltery {

    @Final
    @Shadow(remap = false)
    protected static int ALLOYING_PER_TICK;

    @Final
    @Shadow(remap = false)
    static Logger log;

    @Shadow(remap = false)
    protected SmelteryTank liquids;

    @Inject(method = "alloyAlloys", at = @At("HEAD" ), cancellable = true, remap = false)
    private void injectAlloyAlloys(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.tConstruct.tileSmelteryAlloyRecipeSearch) {
            return;
        }
        ci.cancel();

        if (liquids.getFluidAmount() > liquids.getCapacity()) {
            return;
        }

        Map<Fluid, List<AlloyRecipe>> recipePrefixMap = TinkerRegistryUtils.getAlloyRecipePrefixMap();
        Set<Fluid> fluidSet = stellar_core$buildFluidSet(liquids.getFluids());
        int processedRecipes = 0;
        for (final Fluid fluid : fluidSet) {
            List<AlloyRecipe> recipes = recipePrefixMap.get(fluid);
            if (recipes == null) {
                continue;
            }
            for (final AlloyRecipe recipe : recipes) {
                // find out how often we can apply the recipe
                int matched = recipe.matches(liquids.getFluids());
                if (matched > ALLOYING_PER_TICK) {
                    matched = ALLOYING_PER_TICK;
                }

                if (matched > 0) {
                    processedRecipes++;
                }

                while (matched > 0) {
                    // remove all liquids from the tank
                    for (FluidStack liquid : recipe.getFluids()) {
                        FluidStack toDrain = liquid.copy();
                        FluidStack drained = liquids.drain(toDrain, true);
                        // error logging
                        assert drained != null;
                        if (!drained.isFluidEqual(toDrain) || drained.amount != toDrain.amount) {
                            log.error("Smeltery alloy creation drained incorrect amount: was {}:{}, should be {}:{}", drained
                                    .getUnlocalizedName(), drained.amount, toDrain.getUnlocalizedName(), toDrain.amount);
                        }
                    }

                    // and insert the alloy
                    FluidStack toFill = FluidUtil.getValidFluidStackOrNull(recipe.getResult().copy());
                    int filled = liquids.fill(toFill, true);
                    if (filled != recipe.getResult().amount) {
                        log.error("Smeltery alloy creation filled incorrect amount: was {}, should be {} ({})", filled,
                                recipe.getResult().amount * matched, recipe.getResult().getUnlocalizedName());
                        break;
                    }
                    matched -= filled;
                }

                if (processedRecipes >= StellarCoreConfig.PERFORMANCE.tConstruct.tileSmelteryMaxAlloyRecipePerTick) {
                    return;
                }
            }
        }
    }

    @Unique
    private static Set<Fluid> stellar_core$buildFluidSet(List<FluidStack> fluids) {
        return fluids.stream().map(FluidStack::getFluid).collect(Collectors.toSet());
    }

}
