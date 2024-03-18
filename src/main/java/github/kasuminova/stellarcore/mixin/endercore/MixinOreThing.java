package github.kasuminova.stellarcore.mixin.endercore;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.HashedItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(targets = "com.enderio.core.common.util.stackable.OreThing")
public class MixinOreThing {

    @Nonnull
    @Shadow(remap = false)
    private NonNullList<ItemStack> ores;

    @Unique
    private final Map<HashedItemStack, Boolean> stellar_core$stackCache = new WeakHashMap<>();

    @Inject(method = "is(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    public void injectIs(final ItemStack itemStack, final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.PERFORMANCE.enderCore.oreThing) {
            return;
        }
        if (this.ores.size() <= 4) {
            return;
        }

        if (itemStack == null || itemStack.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        HashedItemStack hashedStack = HashedItemStack.ofMeta(itemStack);
        if (stellar_core$stackCache.containsKey(hashedStack)) {
            cir.setReturnValue(stellar_core$stackCache.get(hashedStack));
        }

        for (ItemStack oreStack : this.ores) {
            if (itemStack.getItem() == oreStack.getItem()
                    && (!oreStack.getHasSubtypes() || oreStack.getItemDamage() == 32767 || oreStack.getMetadata() == itemStack.getMetadata())) {
                stellar_core$stackCache.put(hashedStack, true);
                cir.setReturnValue(true);
                return;
            }
        }

        stellar_core$stackCache.put(hashedStack, false);
        cir.setReturnValue(false);
    }

}
