package github.kasuminova.stellarcore.mixin.enderio;

import crazypants.enderio.base.machine.baselegacy.SlotDefinition;
import crazypants.enderio.base.machine.baselegacy.AbstractInventoryMachineEntity;
import crazypants.enderio.machines.machine.obelisk.base.AbstractMobObeliskEntity;
import github.kasuminova.stellarcore.common.performance.CapturedMobType;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = AbstractMobObeliskEntity.class, remap = false)
public abstract class MixinMobObeliskTypeFilter {
    /**
     * @author StellarCore
     * @reason The filter compares only the captured entity ID, not its serialized state.
     */
    @Overwrite
    protected boolean isMobInFilter(final EntityLivingBase entity) {
        final AbstractInventoryMachineEntity machine = (AbstractInventoryMachineEntity) (Object) this;
        final SlotDefinition slotDefinition = machine.getSlotDefinition();
        for (int slot = slotDefinition.minInputSlot; slot <= slotDefinition.maxInputSlot; slot++) {
            if (CapturedMobType.matches(machine.getStackInSlot(slot), entity)) return true;
        }
        return false;
    }
}
