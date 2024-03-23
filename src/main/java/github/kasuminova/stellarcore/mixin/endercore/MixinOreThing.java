package github.kasuminova.stellarcore.mixin.endercore;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.HashedItemStack;
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
import java.util.WeakHashMap;

@Mixin(targets = "com.enderio.core.common.util.stackable.OreThing")
public class MixinOreThing {

    @Nonnull
    @Shadow(remap = false)
    private NonNullList<ItemStack> ores;

    @Unique
    private final Map<HashedItemStack, Boolean> stellar_core$stackCache = new WeakHashMap<>();
    @Unique
    private final Map<Block, Boolean> stellar_core$blockCache = new WeakHashMap<>();
    @Unique
    private final Map<Item, Boolean> stellar_core$itemCache = new WeakHashMap<>();

    @Inject(method = "is(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    public void injectItemStackIs(final ItemStack itemStack, final CallbackInfoReturnable<Boolean> cir) {
        if (stellar_core$checkShouldCached()) {
            return;
        }

        if (itemStack == null || itemStack.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        HashedItemStack hashedStack = HashedItemStack.ofMeta(itemStack);
        Boolean result = stellar_core$stackCache.get(hashedStack);
        if (result != null) {
            cir.setReturnValue(result);
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

    @Inject(method = "is(Lnet/minecraft/item/Item;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    public void injectItemIs(final Item item, final CallbackInfoReturnable<Boolean> cir) {
        if (stellar_core$checkShouldCached()) {
            return;
        }

        Boolean result = stellar_core$itemCache.get(item);
        if (result != null) {
            cir.setReturnValue(result);
        }

        for (final ItemStack ore : ores) {
            if (item == ore.getItem()) {
                stellar_core$itemCache.put(item, true);
                cir.setReturnValue(true);
                return;
            }
        }
        stellar_core$itemCache.put(item, false);
        cir.setReturnValue(false);
    }

    @Inject(method = "is(Lnet/minecraft/block/Block;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    public void injectBlockIs(final Block block, final CallbackInfoReturnable<Boolean> cir) {
        if (stellar_core$checkShouldCached()) {
            return;
        }

        Boolean result = stellar_core$blockCache.get(block);
        if (result != null) {
            cir.setReturnValue(result);
        }

        for (final ItemStack ore : ores) {
            if (Item.getItemFromBlock(block) == ore.getItem() || Block.getBlockFromItem(ore.getItem()) != block) {
                stellar_core$blockCache.put(block, true);
                cir.setReturnValue(true);
                return;
            }
        }
        stellar_core$blockCache.put(block, false);
        cir.setReturnValue(false);
    }

    @Unique
    private boolean stellar_core$checkShouldCached() {
        if (!StellarCoreConfig.PERFORMANCE.enderCore.oreThing) {
            return true;
        }
        return this.ores.size() <= 4;
    }

}
