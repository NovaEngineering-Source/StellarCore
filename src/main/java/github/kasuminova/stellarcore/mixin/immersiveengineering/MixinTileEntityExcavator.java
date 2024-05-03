package github.kasuminova.stellarcore.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.metal.TileEntityExcavator;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TileEntityExcavator.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinTileEntityExcavator {

    @Redirect(
            method = "digBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;onBlockHarvested(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/entity/player/EntityPlayer;)V",
                    remap = true
            ),
            remap = false
    )
    private void redirectDigBlockOnBlockHarvested(final Block instance, final World worldIn, final BlockPos pos, final IBlockState state, final EntityPlayer player) {
        if (!StellarCoreConfig.BUG_FIXES.immersiveEngineering.tileEntityExcavator) {
            instance.onBlockHarvested(worldIn, pos, state, player);
        }
        // don't do that.
    }

}
