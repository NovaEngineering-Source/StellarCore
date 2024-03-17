package github.kasuminova.stellarcore.mixin.enderioconduits;

import crazypants.enderio.base.capability.ItemTools;
import crazypants.enderio.base.filter.item.IItemFilter;
import crazypants.enderio.conduits.conduit.item.IItemConduit;
import crazypants.enderio.conduits.conduit.item.NetworkedInventory;
import crazypants.enderio.util.Prep;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.CachedItemConduit;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mixin(NetworkedInventory.class)
public abstract class MixinNetworkedInventory {
    @Nonnull
    @Shadow(remap = false) @Final private IItemConduit con;

    @SuppressWarnings({"StaticVariableMayNotBeInitialized", "NonConstantFieldWithUpperCaseName"})
    @Shadow(remap = false) @Final private static boolean EXECUTE;

    @Shadow(remap = false)
    protected abstract int insertIntoTargets(@Nonnull final ItemStack toInsert);

    @Shadow(remap = false)
    protected abstract void onItemExtracted(final int slot, final int numInserted);

    @Shadow(remap = false)
    protected abstract Iterable<Object> getTargetIterator();

    @Shadow(remap = false)
    private static IItemFilter valid(final IItemFilter filter) {
        return null;
    }

    @Shadow(remap = false)
    private static int positive(final int x) {
        return 0;
    }

    @Unique
    private static int stellarcore$insertItemSimulate(NetworkedInventory targetInv, @Nonnull ItemStack item, IItemFilter filter) {
        InvokerNetworkedInventory inv = (InvokerNetworkedInventory) targetInv;
        if (!inv.callCanInsert() || Prep.isInvalid(item)) {
            return 0;
        }
        final IItemHandler inventory = inv.callGetInventory();
        if (inventory == null) {
            return 0;
        }
        if (filter == null) {
            return stellarcore$simulateInsertItem(inventory, item);
        }
        if (filter.isLimited()) {
            final int count = filter.getMaxCountThatPassesFilter(inventory, item);
            if (count <= 0) {
                return 0;
            }
            final int maxInsert = ItemTools.getInsertLimit(inventory, item, count);
            if (maxInsert <= 0) {
                return 0;
            }
            if (maxInsert < item.getCount()) {
                item = item.copy();
                item.setCount(maxInsert);
            }
        } else if (!filter.doesItemPassFilter(inventory, item)) {
            return 0;
        }
        return stellarcore$simulateInsertItem(inventory, item);
    }

    @Unique
    private static int stellarcore$simulateInsertItem(@Nullable IItemHandler inventory, @Nonnull ItemStack item) {
        if (inventory == null || Prep.isInvalid(item)) {
            return 0;
        }
        int startSize = item.getCount();
        ItemStack res = ItemTools.insertItemStacked(inventory, item.copy(), true);
        return startSize - res.getCount();
    }

    @Inject(method = "doTransfer", at = @At("HEAD"), cancellable = true, remap = false)
    public void doTransfer(final IItemHandler inventory,
                           final ItemStack extractedItem,
                           final int slot,
                           final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.BUG_FIXES.enderIOConduits.cachedItemConduit) {
            return;
        }
        if (!(con instanceof CachedItemConduit cachedItemConduit)) {
            return;
        }
        if (!cachedItemConduit.getCachedStack().isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        int simulateInserted = stellarcore$insertIntoTargetsSimulate(extractedItem.copy());
        if (simulateInserted <= 0) {
            cir.setReturnValue(false);
            return;
        }

        ItemStack extracted = inventory.extractItem(slot, simulateInserted, EXECUTE);
        int inserted = insertIntoTargets(extracted.copy());
        int notInserted = extracted.getCount() - inserted;

        if (notInserted > 0) {
            ItemStack notInsertedStack = extracted.copy();
            notInsertedStack.setCount(notInserted);
            cachedItemConduit.setCachedStack(notInsertedStack);
        }

        onItemExtracted(slot, inserted);

        cir.setReturnValue(inserted > 0);
    }

    @Inject(
            method = "transferItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lcrazypants/enderio/conduits/conduit/item/IItemConduit;getMaximumExtracted(Lnet/minecraft/util/EnumFacing;)I",
                    remap = false
            ),
            cancellable = true,
            remap = false)
    public void onTransferItems(final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.BUG_FIXES.enderIOConduits.cachedItemConduit) {
            return;
        }
        if (!(con instanceof CachedItemConduit cachedItemConduit)) {
            return;
        }

        ItemStack cachedStack = cachedItemConduit.getCachedStack();
        if (cachedStack.isEmpty()) {
            return;
        }

        int inserted = insertIntoTargets(cachedStack.copy());
        if (inserted == cachedStack.getCount()) {
            cachedItemConduit.setCachedStack(ItemStack.EMPTY);
            return;
        }
        if (inserted == 0) {
            cir.setReturnValue(false);
            return;
        }
        cachedStack.shrink(inserted);
    }

    @Unique
    private int stellarcore$insertIntoTargetsSimulate(@Nonnull ItemStack toInsert) {
        if (Prep.isInvalid(toInsert)) {
            return 0;
        }

        final int totalToInsert = toInsert.getCount();
        // when true, a sticky filter has claimed this item and so only sticky outputs are allowed to handle it. sticky outputs are first in the target
        // list, so all sticky outputs are queried before any non-sticky one.
        boolean matchedStickyOutput = false;

        for (Object targetObj : getTargetIterator()) {
            AccessorTarget target = (AccessorTarget) targetObj;
            NetworkedInventory inv = target.getInv();
            final IItemFilter filter = valid(inv.getCon().getOutputFilter(inv.getConDir()));
            if (target.getStickyInput() && !matchedStickyOutput && filter != null) {
                matchedStickyOutput = filter.doesItemPassFilter(inv.getInventory(), toInsert);
            }
            if (target.getStickyInput() || !matchedStickyOutput) {
                toInsert.shrink(positive(stellarcore$insertItemSimulate(inv, toInsert, filter)));
                if (Prep.isInvalid(toInsert)) {
                    // everything has been inserted. we're done.
                    break;
                }
                continue;
            }
            if (!target.getStickyInput()) {
                // item has been claimed by a sticky output but there are no sticky outputs left in targets, so we can stop checking
                break;
            }
        }

        return totalToInsert - toInsert.getCount();
    }
}
