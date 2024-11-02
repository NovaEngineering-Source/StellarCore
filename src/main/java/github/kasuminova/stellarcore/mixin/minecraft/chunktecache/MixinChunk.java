package github.kasuminova.stellarcore.mixin.minecraft.chunktecache;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import github.kasuminova.stellarcore.common.util.BlockPosSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@SuppressWarnings("DataFlowIssue")
@Mixin(Chunk.class)
public class MixinChunk {

    @Final
    @Shadow
    private World world;

    @Unique
    private final Set<BlockPos> stellar_core$invalidTESet = BlockPosSet.create();

    @Inject(method = "createNewTileEntity", at = @At("HEAD"), cancellable = true)
    private void injectCreateNewTileEntity(final BlockPos pos, final CallbackInfoReturnable<TileEntity> cir) {
        if (world.isRemote || !world.getMinecraftServer().isCallingFromMinecraftThread()) {
            return;
        }
        if (this.stellar_core$invalidTESet.contains(pos)) {
            cir.setReturnValue(null);
        }
    }

    @ModifyReturnValue(method = "createNewTileEntity", at = @At("RETURN"))
    private TileEntity injectCreateNewTileEntityReturn(final TileEntity ret, final BlockPos pos) {
        if (world.isRemote || !world.getMinecraftServer().isCallingFromMinecraftThread()) {
            return ret;
        }
        if (ret == null) {
            this.stellar_core$invalidTESet.add(pos);
        }
        return ret;
    }

    @Inject(
            method = "setBlockState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;set(IIILnet/minecraft/block/state/IBlockState;)V"
            )
    )
    private void injectSetBlockState(final BlockPos pos, final IBlockState state, final CallbackInfoReturnable<IBlockState> cir) {
        if (world.isRemote) {
            return;
        }
        if (state.getBlock().hasTileEntity(state)) {
            this.stellar_core$invalidTESet.remove(pos);
        } else {
            this.stellar_core$invalidTESet.add(pos);
        }
    }

}
