package github.kasuminova.stellarcore.mixin.endercore;

import com.enderio.core.common.util.NNList;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.HashedItemStack;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;

@SuppressWarnings("JavadocReference")
@Mixin(targets = "com.enderio.core.common.util.stackable.OreThing")
public class MixinOreThing {

    @Nonnull
    @Shadow(remap = false)
    private NonNullList<ItemStack> ores;

    @Unique
    private ObjectSet<Item> stellar_core$hashedItem = null;
    @Unique
    private ObjectSet<HashedItemStack> stellar_core$hashedItemWithMeta = null;

    /**
     * Only use for {@link com.enderio.core.common.util.stackable.OreThing#is(Item item)}
     */
    @Unique
    private ObjectSet<Item> stellar_core$itemCache = null;

    @SuppressWarnings("rawtypes")
    @Inject(method = "bake", at = @At("RETURN"), remap = false)
    public void injectBake(final CallbackInfoReturnable<NNList> cir) {
        stellar_core$hashedItem = new ObjectOpenHashSet<>();
        stellar_core$hashedItemWithMeta = new ObjectOpenHashSet<>();
        for (final ItemStack ore : ores) {
            if (!ore.getHasSubtypes() || ore.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                stellar_core$hashedItem.add(ore.getItem());
            } else {
                stellar_core$hashedItemWithMeta.add(HashedItemStack.ofMetaUnsafe(ore));
            }
        }

        stellar_core$itemCache = new ObjectOpenHashSet<>();
        for (final ItemStack ore : ores) {
            stellar_core$itemCache.add(ore.getItem());
        }
    }

    @Inject(method = "is(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    public void injectItemStackIs(final ItemStack stack, final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.PERFORMANCE.enderCore.oreThing) {
            return;
        }

        if (stack == null || stack.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        Item item = stack.getItem();
        cir.setReturnValue(stellar_core$hashedItem.contains(item) || stellar_core$hashedItemWithMeta.contains(HashedItemStack.ofMetaUnsafe(stack)));
    }

    @Inject(method = "is(Lnet/minecraft/item/Item;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    public void injectItemIs(final Item item, final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.PERFORMANCE.enderCore.oreThing) {
            return;
        }

        cir.setReturnValue(stellar_core$itemCache.contains(item));
    }

}
