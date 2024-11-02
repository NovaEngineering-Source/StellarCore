package github.kasuminova.stellarcore.mixin.psi;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.psi.common.spell.trick.block.PieceTrickPlaceBlock;

@Mixin(PieceTrickPlaceBlock.class)
public abstract class MixinPieceTrickPlaceBlock {

    @Inject(
            method = "placeBlock(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;IZZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lvazkii/psi/common/spell/trick/block/PieceTrickPlaceBlock;removeFromInventory(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"
            ),
            remap = false,
            cancellable = true
    )
    private static void injectPlaceBlock(final EntityPlayer player, final World world, final BlockPos pos, final int slot, final boolean particles, final boolean conjure, final CallbackInfo ci) {
        ItemStack itemBlockStack = player.inventory.getStackInSlot(slot);
        ItemBlock itemBlock = (ItemBlock) itemBlockStack.getItem();
        EnumHand hand = player.getActiveHand();
        switch (hand) {
            case MAIN_HAND -> stellar_core$swapItemStack(player, player.inventory.currentItem, slot);
            case OFF_HAND -> stellar_core$swapItemStack(player, 0, slot);
        }

        if (!world.mayPlace(itemBlock.getBlock(), pos, false, EnumFacing.UP, player)) {
            ci.cancel();
        }

        switch (hand) {
            case MAIN_HAND -> stellar_core$swapItemStack(player, player.inventory.currentItem, slot);
            case OFF_HAND -> stellar_core$swapItemStack(player, 0, slot);
        }
    }

    @Unique
    private static void stellar_core$swapItemStack(final EntityPlayer player, final int currItemIdx, final int slot) {
        InventoryPlayer inventory = player.inventory;

        ItemStack tmp = inventory.getStackInSlot(slot);
        inventory.setInventorySlotContents(slot, inventory.getStackInSlot(currItemIdx));
        inventory.setInventorySlotContents(currItemIdx, tmp);
    }

}
