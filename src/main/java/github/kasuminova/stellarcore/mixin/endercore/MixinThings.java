package github.kasuminova.stellarcore.mixin.endercore;

import com.enderio.core.common.util.stackable.Things;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.HashedItemStack;
import github.kasuminova.stellarcore.mixin.util.IIThing;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(Things.class)
public class MixinThings {

    @Final
    @Nonnull
    @Shadow(remap = false)
    private List<Object> things;

    @Shadow(remap = false)
    private NBTTagCompound nbt;

    @Unique
    private final Map<HashedItemStack, Boolean> stellar_core$itemStackCache = new WeakHashMap<>();

    @Unique
    private final Map<Item, Boolean> stellar_core$itemCache = new WeakHashMap<>();

    @Unique
    private final Map<Block, Boolean> stellar_core$blockCache = new WeakHashMap<>();

    @Inject(method = "contains(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectItemStackContains(final ItemStack itemStack, final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.PERFORMANCE.enderCore.things) {
            return;
        }
        if (itemStack == null || itemStack.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        HashedItemStack stack = HashedItemStack.ofTagUnsafe(itemStack);
        Boolean cache = stellar_core$itemStackCache.get(stack);
        if (cache != null) {
            cir.setReturnValue(cache);
            return;
        }

        for (final Object iThing : this.things) {
            IIThing thing = (IIThing) iThing;
            if (thing.callIs(itemStack) && (this.nbt == null || this.nbt.equals(itemStack.getTagCompound()))) {
                stellar_core$itemStackCache.put(stack.copy(), true);
                cir.setReturnValue(true);
                return;
            }
        }

        stellar_core$itemStackCache.put(stack, false);
        cir.setReturnValue(false);
    }

    @Inject(method = "contains(Lnet/minecraft/item/Item;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectItemContains(final Item item, final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.PERFORMANCE.enderCore.things) {
            return;
        }
        if (item == null || item != Items.AIR) {
            cir.setReturnValue(false);
            return;
        }

        Boolean cache = stellar_core$itemCache.get(item);
        if (cache != null) {
            cir.setReturnValue(cache);
            return;
        }

        for (final Object iThing : this.things) {
            IIThing thing = (IIThing) iThing;
            if (thing.callIs(item)) {
                stellar_core$itemCache.put(item, true);
                cir.setReturnValue(true);
                return;
            }
        }

        stellar_core$itemCache.put(item, false);
        cir.setReturnValue(false);
    }

    @Inject(method = "contains(Lnet/minecraft/block/Block;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectItemContains(final Block block, final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.PERFORMANCE.enderCore.things) {
            return;
        }
        if (block == null || block == Blocks.AIR) {
            cir.setReturnValue(false);
            return;
        }

        Boolean cache = stellar_core$blockCache.get(block);
        if (cache != null) {
            cir.setReturnValue(cache);
            return;
        }

        for (final Object iThing : this.things) {
            IIThing thing = (IIThing) iThing;
            if (thing.callIs(block)) {
                stellar_core$blockCache.put(block, true);
                cir.setReturnValue(true);
                return;
            }
        }

        stellar_core$blockCache.put(block, false);
        cir.setReturnValue(false);
    }

    @Inject(method = "cleanCachedValues", at = @At("RETURN"), remap = false)
    private void injectCleanCachedValues(final CallbackInfo ci) {
        stellar_core$itemStackCache.clear();
        stellar_core$itemCache.clear();
        stellar_core$blockCache.clear();
    }

}
