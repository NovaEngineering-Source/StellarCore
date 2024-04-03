package github.kasuminova.stellarcore.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.BlockIEMultiblock;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.Iterator;

@Mixin(BlockIEMultiblock.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinBlockIEMultiblock {

    @Redirect(
            method = "breakBlock",
            at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z", remap = false)
    )
    @SuppressWarnings("rawtypes")
    private boolean onBreakBlock(final Iterator instance, @Local(name = "master") IIEInventory master) {
        if (instance.hasNext()) {
            return true;
        }
        if (!StellarCoreConfig.BUG_FIXES.immersiveEngineering.blockIEMultiblock) {
            return false;
        }
        NonNullList<ItemStack> inventory = master.getInventory();
        if (inventory == null || inventory.isEmpty()) {
            return false;
        }
        Collections.fill(inventory, ItemStack.EMPTY);
        return false;
    }

}
