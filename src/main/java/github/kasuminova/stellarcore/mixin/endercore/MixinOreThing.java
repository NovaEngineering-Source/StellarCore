package github.kasuminova.stellarcore.mixin.endercore;

import com.enderio.core.common.util.NNList;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.HashedItemStack;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
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
import java.util.Set;
import java.util.WeakHashMap;

@Mixin(targets = "com.enderio.core.common.util.stackable.OreThing")
public class MixinOreThing {

    @Nonnull
    @Shadow(remap = false)
    private NonNullList<ItemStack> ores;

//    @Unique
//    private final Map<Block, Boolean> stellar_core$blockCache = new WeakHashMap<>();

    @Unique
    private ObjectSet<HashedItemStack> stellar_core$hashedOres = null;
    @Unique
    private ObjectSet<Item> stellar_core$itemCache = null;

    @SuppressWarnings("rawtypes")
    @Inject(method = "bake", at = @At("RETURN"), remap = false)
    public void injectBake(final CallbackInfoReturnable<NNList> cir) {
        stellar_core$hashedOres = new ObjectOpenHashSet<>();
        for (final ItemStack ore : ores) {
            stellar_core$hashedOres.add(HashedItemStack.ofMetaUnsafe(ore));
        }
        stellar_core$itemCache = new ObjectOpenHashSet<>();
        for (final ItemStack ore : ores) {
            stellar_core$itemCache.add(ore.getItem());
        }
    }

    @Inject(method = "is(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    public void injectItemStackIs(final ItemStack stack, final CallbackInfoReturnable<Boolean> cir) {
        if (stellar_core$checkShouldCached()) {
            return;
        }

        if (stack == null || stack.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        cir.setReturnValue(stellar_core$hashedOres.contains(HashedItemStack.ofMetaUnsafe(stack)));
    }

    @Inject(method = "is(Lnet/minecraft/item/Item;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    public void injectItemIs(final Item item, final CallbackInfoReturnable<Boolean> cir) {
        if (stellar_core$checkShouldCached()) {
            return;
        }

        cir.setReturnValue(stellar_core$itemCache.contains(item));
    }

//    @Inject(method = "is(Lnet/minecraft/block/Block;)Z", at = @At("HEAD"), cancellable = true, remap = false)
//    public void injectBlockIs(final Block block, final CallbackInfoReturnable<Boolean> cir) {
//        if (stellar_core$checkShouldCached()) {
//            return;
//        }
//
//        Boolean result = stellar_core$blockCache.get(block);
//        if (result != null) {
//            cir.setReturnValue(result);
//        }
//
//        for (final ItemStack ore : ores) {
//            if (Item.getItemFromBlock(block) == ore.getItem() || Block.getBlockFromItem(ore.getItem()) != block) {
//                stellar_core$blockCache.put(block, true);
//                cir.setReturnValue(true);
//                return;
//            }
//        }
//        stellar_core$blockCache.put(block, false);
//        cir.setReturnValue(false);
//    }

    @Unique
    private boolean stellar_core$checkShouldCached() {
        if (!StellarCoreConfig.PERFORMANCE.enderCore.oreThing) {
            return true;
        }
        return this.ores.size() <= 4;
    }

}
