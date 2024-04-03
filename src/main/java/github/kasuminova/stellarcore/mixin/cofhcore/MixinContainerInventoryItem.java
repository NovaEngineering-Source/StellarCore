package github.kasuminova.stellarcore.mixin.cofhcore;

import cofh.core.gui.container.ContainerInventoryItem;
import cofh.core.gui.slot.SlotLocked;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerInventoryItem.class)
public abstract class MixinContainerInventoryItem extends Container {

    @Shadow(remap = false) @Final protected int containerIndex;

    @Inject(method = "slotClick", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectSlotClick(final int slotIndex,
                                 final int mouseButton,
                                 final ClickType modifier,
                                 final EntityPlayer player,
                                 final CallbackInfoReturnable<ItemStack> cir)
    {
        if (!StellarCoreConfig.BUG_FIXES.coFHCore.containerInventoryItem) {
            return;
        }
        if (slotIndex >= 0 && slotIndex < this.inventorySlots.size()) {
            if (slotIndex == this.containerIndex || getSlot(slotIndex) instanceof SlotLocked) {
                cir.setReturnValue(ItemStack.EMPTY);
            }
        }
        if (modifier == ClickType.SWAP && mouseButton >= 0 && mouseButton < 9) {
            if (mouseButton == this.containerIndex || getSlot(mouseButton) instanceof SlotLocked) {
                cir.setReturnValue(ItemStack.EMPTY);
            }
        }
    }

}
