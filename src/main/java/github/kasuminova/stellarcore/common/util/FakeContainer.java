package github.kasuminova.stellarcore.common.util;

import github.kasuminova.stellarcore.common.util.func.Consumer4;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class FakeContainer extends Container {

    private Consumer<IInventory> onCraftMatrixChanged = null;
    
    private Consumer4<World, EntityPlayer, InventoryCrafting, InventoryCraftResult> onSlotChangedCraftingGrid = null;

    public void setOnCraftMatrixChanged(final Consumer<IInventory> onCraftMatrixChanged) {
        this.onCraftMatrixChanged = onCraftMatrixChanged;
    }

    public void setOnSlotChangedCraftingGrid(final Consumer4<World, EntityPlayer, InventoryCrafting, InventoryCraftResult> onSlotChangedCraftingGrid) {
        this.onSlotChangedCraftingGrid = onSlotChangedCraftingGrid;
    }

    @Override
    public boolean canInteractWith(@Nonnull final EntityPlayer playerIn) {
        return true;
    }

    @Override
    public void onCraftMatrixChanged(@Nonnull final IInventory inventory) {
        if (onCraftMatrixChanged != null) {
            onCraftMatrixChanged.accept(inventory);
        }
        super.onCraftMatrixChanged(inventory);
    }

    @Override
    protected void slotChangedCraftingGrid(@Nonnull final World world,
                                           @Nonnull final EntityPlayer player,
                                           @Nonnull final InventoryCrafting crafting,
                                           @Nonnull final InventoryCraftResult craftResult)
    {
        if (onSlotChangedCraftingGrid != null) {
            onSlotChangedCraftingGrid.accept(world, player, crafting, craftResult);
        }
        super.slotChangedCraftingGrid(world, player, crafting, craftResult);
    }

}
