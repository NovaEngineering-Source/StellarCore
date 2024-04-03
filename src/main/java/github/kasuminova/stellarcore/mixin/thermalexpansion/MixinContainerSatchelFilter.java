package github.kasuminova.stellarcore.mixin.thermalexpansion;

import cofh.core.gui.container.ContainerCore;
import cofh.core.gui.slot.SlotLocked;
import cofh.thermalexpansion.gui.container.storage.ContainerSatchelFilter;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import hellfirepvp.astralsorcery.common.container.ContainerSlotUnclickable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nonnull;

@Mixin(ContainerSatchelFilter.class)
public abstract class MixinContainerSatchelFilter extends ContainerCore {

    @Shadow(remap = false) @Final protected int containerIndex;

    @Unique
    @Nonnull
    @Override
    public ItemStack slotClick(final int slotId,
                               final int dragType,
                               @Nonnull final ClickType clickTypeIn,
                               @Nonnull final EntityPlayer player)
    {
        if (!StellarCoreConfig.BUG_FIXES.thermalExpansion.containerSatchelFilter) {
            return super.slotClick(slotId, dragType, clickTypeIn, player);
        }
        if (slotId >= 0 && slotId < this.inventorySlots.size()) {
            if (slotId == this.containerIndex || getSlot(slotId) instanceof SlotLocked) {
                return ItemStack.EMPTY;
            }
        }
        if (clickTypeIn == ClickType.SWAP && dragType >= 0 && dragType < 9) {
            if (dragType == this.containerIndex || getSlot(dragType) instanceof SlotLocked) {
                return ItemStack.EMPTY;
            }
        }
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

}
