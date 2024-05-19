package github.kasuminova.stellarcore.mixin.ic2;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import ic2.core.block.BlockTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(BlockTileEntity.class)
public abstract class MixinBlockTileEntity {

//    @Shadow(remap = false)
//    public abstract List<ItemStack> getDrops(final IBlockAccess world, final BlockPos pos, final IBlockState state, final int fortune);
//
//    @Unique
//    private static boolean stellar_core$shouldGetDrops = false;
//
//    @Inject(method = "breakBlock", at = @At("RETURN"))
//    private void injectBreakBlock(final World world, final BlockPos pos, final IBlockState state, final CallbackInfo ci) {
//        if (!StellarCoreConfig.BUG_FIXES.industrialCraft2.blockTileEntityDrop) {
//            return;
//        }
//        stellar_core$shouldGetDrops = true;
//        getDrops(world, pos, state, 0).forEach(drop -> Block.spawnAsEntity(world, pos, drop));
//        stellar_core$shouldGetDrops = false;
//    }
//
//    @Inject(
//            method = "getDrops(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Ljava/util/List;",
//            at = @At("HEAD"),
//            cancellable = true,
//            remap = false
//    )
//    private void injectGetDrops(final IBlockAccess world, final BlockPos pos, final IBlockState state, final int fortune, final CallbackInfoReturnable<List<ItemStack>> cir) {
//        if (!StellarCoreConfig.BUG_FIXES.industrialCraft2.blockTileEntityDrop) {
//            return;
//        }
//        if (!stellar_core$shouldGetDrops) {
//            cir.setReturnValue(Collections.emptyList());
//        }
//    }

}
