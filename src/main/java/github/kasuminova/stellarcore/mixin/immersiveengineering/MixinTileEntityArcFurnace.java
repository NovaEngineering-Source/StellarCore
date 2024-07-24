package github.kasuminova.stellarcore.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.metal.TileEntityArcFurnace;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Mixin(value = TileEntityArcFurnace.class, remap = false)
public class MixinTileEntityArcFurnace {

    @Shadow
    IItemHandler inputHandler;

    @Shadow
    public NonNullList<ItemStack> inventory;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        if (!StellarCoreConfig.BUG_FIXES.immersiveEngineering.tileEntityArcFurnace) {
            return;
        }
        this.inputHandler = new IItemHandler() {
            private final IItemHandler parent = inputHandler;

            @Override
            public int getSlots() {
                return parent.getSlots();
            }

            @Nonnull
            @Override
            public ItemStack getStackInSlot(final int slot) {
                return parent.getStackInSlot(slot);
            }

            @Nonnull
            @Override
            public ItemStack insertItem(final int slot, @Nonnull ItemStack stack, final boolean simulate) {
                if (stack.isEmpty()) {
                    return stack;
                }
                stack = stack.copy();

                List<Integer> possibleSlots = new ArrayList<>(12);
                for (int i = 0; i < 12; i++) {
                    ItemStack here = inventory.get(i);
                    if (here.isEmpty()) {
                        if (!simulate) {
                            int maxStackSize = stack.getMaxStackSize();
                            if (stack.getCount() > maxStackSize) {
                                here = stack.copy();
                                here.setCount(maxStackSize);
                                stack.shrink(maxStackSize);
                                inventory.set(i, here);
                                if (stack.isEmpty()) {
                                    return ItemStack.EMPTY;
                                }
                            } else {
                                inventory.set(i, stack);
                                return ItemStack.EMPTY;
                            }
                        }
                        continue;
                    }

                    if (ItemHandlerHelper.canItemStacksStack(stack, here) && here.getCount() < here.getMaxStackSize()) {
                        possibleSlots.add(i);
                    }
                }

                possibleSlots.sort((a, b) -> Integer.compare(inventory.get(a).getCount(), inventory.get(b).getCount()));

                for (int i : possibleSlots) {
                    ItemStack here = inventory.get(i);
                    int fillCount = Math.min(here.getMaxStackSize() - here.getCount(), stack.getCount());
                    if (!simulate) {
                        here.grow(fillCount);
                    }
                    stack.shrink(fillCount);
                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
                return stack;
            }

            @Nonnull
            @Override
            public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
                return parent.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(final int slot) {
                return parent.getSlotLimit(slot);
            }
        };
    }

}
