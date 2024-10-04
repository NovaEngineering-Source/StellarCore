package github.kasuminova.stellarcore.mixin.avaritia;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarEnvironment;
import morph.avaritia.recipe.AvaritiaRecipeManager;
import morph.avaritia.recipe.IRecipeFactory;
import morph.avaritia.recipe.compressor.ICompressorRecipe;
import morph.avaritia.recipe.extreme.IExtremeRecipe;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings({"StaticVariableMayNotBeInitialized", "SynchronizeOnNonFinalField", "NonConstantFieldWithUpperCaseName", "rawtypes", "unchecked"})
@Mixin(value = AvaritiaRecipeManager.class, remap = false)
public class MixinAvaritiaRecipeManager {

    @Final
    @Shadow
    private static Map<ResourceLocation, IRecipeFactory<IExtremeRecipe>> extremeRecipeFactories;

    @Final
    @Shadow
    public static Map<ResourceLocation, IExtremeRecipe> EXTREME_RECIPES;

    @Final
    @Shadow
    public static Map<ResourceLocation, ICompressorRecipe> COMPRESSOR_RECIPES;

    @Redirect(
            method = "loadFactories",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private static Object redirectLoadFactoriesPut(final Map<Object, Object> instance, final Object k, final Object v) {
        if (!StellarCoreConfig.PERFORMANCE.avaritia.avaritiaRecipeManager) {
            return instance.put(k, v);
        }
        synchronized (EXTREME_RECIPES) {
            return instance.put(k, v);
        }
    }

    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"
            )
    )
    private static void injectInitForEach(final List instance, final Consumer consumer) {
        if (!StellarCoreConfig.PERFORMANCE.avaritia.avaritiaRecipeManager || !StellarEnvironment.shouldParallel()) {
            instance.forEach(consumer);
            return;
        }
        instance.parallelStream().forEach(consumer);
    }

    @Redirect(
            method = "lambda$null$1",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private static Object injectInitPut0(final Map<Object, Object> instance, final Object k, final Object v) {
        if (!StellarCoreConfig.PERFORMANCE.avaritia.avaritiaRecipeManager) {
            return instance.put(k, v);
        }
        synchronized (extremeRecipeFactories) {
            return instance.put(k, v);
        }
    }

    @Redirect(
            method = "lambda$null$3",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private static Object injectInitPut1(final Map<Object, Object> instance, final Object k, final Object v) {
        if (!StellarCoreConfig.PERFORMANCE.avaritia.avaritiaRecipeManager) {
            return instance.put(k, v);
        }
        synchronized (COMPRESSOR_RECIPES) {
            return instance.put(k, v);
        }
    }

}
