package github.kasuminova.stellarcore.mixin.astralsorcery;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import hellfirepvp.astralsorcery.common.container.ContainerJournal;
import hellfirepvp.astralsorcery.common.container.ContainerSlotUnclickable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nonnull;

@Mixin(ContainerJournal.class)
public abstract class MixinContainerJournal extends Container {

    @Shadow(remap = false) @Final private int journalIndex;

    @Unique
    @Nonnull
    @Override
    public ItemStack slotClick(final int slotId,
                               final int dragType,
                               @Nonnull final ClickType clickTypeIn,
                               @Nonnull final EntityPlayer player)
    {
        if (!StellarCoreConfig.BUG_FIXES.astralSorcery.containerJournal) {
            return super.slotClick(slotId, dragType, clickTypeIn, player);
        }
        if (slotId >= 0 && slotId < this.inventorySlots.size()) {
            if (slotId == this.journalIndex || getSlot(slotId) instanceof ContainerSlotUnclickable) {
                return ItemStack.EMPTY;
            }
        }
        if (clickTypeIn == ClickType.SWAP && dragType >= 0 && dragType < 9) {
            if (dragType == this.journalIndex || getSlot(dragType) instanceof ContainerSlotUnclickable) {
                return ItemStack.EMPTY;
            }
        }
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

}
