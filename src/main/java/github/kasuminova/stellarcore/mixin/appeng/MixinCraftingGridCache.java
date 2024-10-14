package github.kasuminova.stellarcore.mixin.appeng;

import appeng.me.cache.CraftingGridCache;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.itemstack.ItemStackCapInitializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = CraftingGridCache.class, remap = false)
public class MixinCraftingGridCache {

    /**
     * Prevents a large number of ItemStacks from loading.
     */
    @Inject(method = "onUpdateTick", at = @At("HEAD"))
    private void injectOnUpdateTickStart(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.asyncItemStackCapabilityInit) {
            return;
        }
        ItemStackCapInitializer.setShouldAddTask(false);
    }

    /**
     * Prevents a large number of ItemStacks from loading.
     */
    @Inject(method = "onUpdateTick", at = @At("RETURN"))
    private void injectOnUpdateTickEnd(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.asyncItemStackCapabilityInit) {
            return;
        }
        ItemStackCapInitializer.setShouldAddTask(true);
    }

}
