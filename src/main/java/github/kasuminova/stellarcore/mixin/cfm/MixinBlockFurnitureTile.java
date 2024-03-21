package github.kasuminova.stellarcore.mixin.cfm;

import com.llamalad7.mixinextras.sugar.Local;
import com.mrcrayfish.furniture.blocks.BlockFurnitureTile;
import com.mrcrayfish.furniture.gui.inventory.ISimpleInventory;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.IntStream;

@Mixin(BlockFurnitureTile.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinBlockFurnitureTile {

    @Inject(method = "breakBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/inventory/InventoryHelper;dropInventoryItems(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/inventory/IInventory;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void injectDropInventoryItemsVanilla(final World world, final BlockPos pos, final IBlockState state, final CallbackInfo ci, @Local(name = "inv") IInventory inv) {
        if (!StellarCoreConfig.BUG_FIXES.mrCrayfishFurniture.blockFurnitureTile) {
            return;
        }
        IntStream.range(0, inv.getSizeInventory())
                .filter(i -> !inv.getStackInSlot(i).isEmpty())
                .forEach(i -> inv.setInventorySlotContents(i, ItemStack.EMPTY));
    }

    @Inject(method = "breakBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mrcrayfish/furniture/util/InventoryUtil;dropInventoryItems(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lcom/mrcrayfish/furniture/gui/inventory/ISimpleInventory;)V",
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            remap = true
    )
    private void injectDropInventoryItems(final World world, final BlockPos pos, final IBlockState state, final CallbackInfo ci, @Local(name = "inv") ISimpleInventory inv) {
        if (!StellarCoreConfig.BUG_FIXES.mrCrayfishFurniture.blockFurnitureTile) {
            return;
        }
        inv.clear();
    }

}
