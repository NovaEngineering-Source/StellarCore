package github.kasuminova.stellarcore.mixin.ic2;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import ic2.core.item.BaseElectricItem;
import ic2.core.item.ItemIC2;
import ic2.core.item.ItemToolIC2;
import ic2.core.item.tool.HarvestLevel;
import ic2.core.item.tool.IToolClass;
import ic2.core.item.tool.ItemElectricTool;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;
import java.util.Set;

@Mixin(ItemElectricTool.class)
public abstract class MixinItemElectricTool extends ItemToolIC2 {

    @Shadow(remap = false)
    public abstract double getMaxCharge(final ItemStack stack);

    protected MixinItemElectricTool(final ItemName name, final HarvestLevel harvestLevel, final Set<? extends IToolClass> toolClasses) {
        super(name, harvestLevel, toolClasses);
    }

    @Override
    public boolean showDurabilityBar(@Nonnull final ItemStack stack) {
        if (!StellarCoreConfig.FEATURES.ic2.electricItemNonDurability) {
            return super.showDurabilityBar(stack);
        }
        return true;
    }

    @Override
    public double getDurabilityForDisplay(@Nonnull final ItemStack stack) {
        if (!StellarCoreConfig.FEATURES.ic2.electricItemNonDurability) {
            return super.getDurabilityForDisplay(stack);
        }

        NBTTagCompound tNBT = stack.getTagCompound();
        double charge = tNBT == null ? 0D : tNBT.getDouble("charge");
        double maxCharge = getMaxCharge(stack);
        if (maxCharge > 0.0D) {
            return 1D - charge / maxCharge;
        }
        return super.getDurabilityForDisplay(stack);
    }

    @Override
    public int getRGBDurabilityForDisplay(@Nonnull final ItemStack stack) {
        if (!StellarCoreConfig.FEATURES.ic2.electricItemNonDurability) {
            return super.getRGBDurabilityForDisplay(stack);
        }
        return 0x87CEFA;
    }

}
