package github.kasuminova.stellarcore.mixin.minecraft.forge;

import com.google.common.collect.Lists;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.BlockSnapShotProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ForgeHooks.class)
public class MixinForgeHooks {

    /**
     * @author Kasumi_Nova
     * @reason 重写获取的 capturedBlockSnapshots 捏，有一定危险性。
     */
    @Inject(method = "onPlaceItemIntoWorld", at = @At("HEAD"), cancellable = true, remap = false)
    @SuppressWarnings("unchecked")
    private static void onPlaceItemIntoWorld(final ItemStack itemstack,
                                             final EntityPlayer player,
                                             final World world,
                                             final BlockPos pos,
                                             final EnumFacing side,
                                             final float hitX,
                                             final float hitY,
                                             final float hitZ,
                                             final EnumHand hand,
                                             final CallbackInfoReturnable<EnumActionResult> cir)
    {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.capturedBlockSnapshots) {
            return;
        }

        // handle all placement events here
        int meta = itemstack.getItemDamage();
        int size = itemstack.getCount();
        NBTTagCompound nbt = null;
        if (itemstack.getTagCompound() != null)
        {
            nbt = itemstack.getTagCompound().copy();
        }

        if (!(itemstack.getItem() instanceof ItemBucket)) // if not bucket
        {
            world.captureBlockSnapshots = true;
        }

        EnumActionResult ret = itemstack.getItem().onItemUse(player, world, pos, hand, side, hitX, hitY, hitZ);
        world.captureBlockSnapshots = false;

        if (ret == EnumActionResult.SUCCESS)
        {
            // save new item data
            int newMeta = itemstack.getItemDamage();
            int newSize = itemstack.getCount();
            NBTTagCompound newNBT = null;
            if (itemstack.getTagCompound() != null)
            {
                newNBT = itemstack.getTagCompound().copy();
            }
            BlockEvent.PlaceEvent placeEvent = null;
            List<BlockSnapshot> blockSnapshots;
            if (world instanceof BlockSnapShotProvider snapShotProvider) {
                blockSnapshots = (List<BlockSnapshot>) snapShotProvider.getCapturedBlockSnapshots().clone();
                snapShotProvider.getCapturedBlockSnapshots().clear();
            } else {
                blockSnapshots = (List<BlockSnapshot>) world.capturedBlockSnapshots.clone();
                world.capturedBlockSnapshots.clear();
            }

            // make sure to set pre-placement item data for event
            itemstack.setItemDamage(meta);
            itemstack.setCount(size);
            if (nbt != null)
            {
                itemstack.setTagCompound(nbt);
            }
            if (blockSnapshots.size() > 1)
            {
                placeEvent = ForgeEventFactory.onPlayerMultiBlockPlace(player, blockSnapshots, side, hand);
            }
            else if (blockSnapshots.size() == 1)
            {
                placeEvent = ForgeEventFactory.onPlayerBlockPlace(player, blockSnapshots.get(0), side, hand);
            }

            if (placeEvent != null && placeEvent.isCanceled())
            {
                ret = EnumActionResult.FAIL; // cancel placement
                // revert back all captured blocks
                for (BlockSnapshot blocksnapshot : Lists.reverse(blockSnapshots))
                {
                    world.restoringBlockSnapshots = true;
                    blocksnapshot.restore(true, false);
                    world.restoringBlockSnapshots = false;
                }
            }
            else
            {
                // Change the stack to its new content
                itemstack.setItemDamage(newMeta);
                itemstack.setCount(newSize);
                if (nbt != null)
                {
                    itemstack.setTagCompound(newNBT);
                }

                for (BlockSnapshot snap : blockSnapshots)
                {
                    int updateFlag = snap.getFlag();
                    IBlockState oldBlock = snap.getReplacedBlock();
                    IBlockState newBlock = world.getBlockState(snap.getPos());
                    if (!newBlock.getBlock().hasTileEntity(newBlock)) // Containers get placed automatically
                    {
                        newBlock.getBlock().onBlockAdded(world, snap.getPos(), newBlock);
                    }

                    world.markAndNotifyBlock(snap.getPos(), null, oldBlock, newBlock, updateFlag);
                }
                player.addStat(StatList.getObjectUseStats(itemstack.getItem()));
            }
        }
        if (world instanceof BlockSnapShotProvider snapShotProvider) {
            snapShotProvider.getCapturedBlockSnapshots().clear();
        } else {
            world.capturedBlockSnapshots.clear();
        }

        cir.setReturnValue(ret);
    }

}
