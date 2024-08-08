package github.kasuminova.stellarcore.mixin.enderio;

import crazypants.enderio.base.farming.registry.Commune;
import crazypants.enderio.base.farming.registry.Registry;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.HashedItemStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(value = Commune.class, remap = false)
public class MixinCommune {

    @Unique
    private final Map<HashedItemStack, Boolean> stellar_core$cache = new WeakHashMap<>();

    @Inject(method = "canPlant", at = @At("HEAD"), cancellable = true)
    private void injectCanPlant(final ItemStack stack, final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.PERFORMANCE.enderIO.commune) {
            return;
        }

        HashedItemStack hashedStack = HashedItemStack.ofTagUnsafe(stack);
        Boolean cache = stellar_core$cache.get(hashedStack);
        if (cache != null) {
            cir.setReturnValue(cache);
            return;
        }

        boolean result = Registry.foreach(joe -> joe.canPlant(stack) ? Boolean.TRUE : null) != null;
        stellar_core$cache.put(hashedStack.copy(), result);
        cir.setReturnValue(result);
    }

}
