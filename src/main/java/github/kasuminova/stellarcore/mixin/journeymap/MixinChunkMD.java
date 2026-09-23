package github.kasuminova.stellarcore.mixin.journeymap;

import journeymap.client.data.DataCache;
import journeymap.client.model.chunk.ChunkMD;
import journeymap.common.Journeymap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ChunkMD.class, remap = false)
public abstract class MixinChunkMD {

    @Shadow public abstract int toWorldX(int localX);

    @Shadow public abstract int toWorldZ(int localZ);

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Ljourneymap/client/model/chunk/ChunkMD;getBlockPos(III)Lnet/minecraft/util/math/BlockPos;"))
    public BlockPos getBlockPos(ChunkMD instance, int localX, int y, int localZ) {
        var pos = BlockPos.PooledMutableBlockPos.retain(this.toWorldX(localX), y, this.toWorldZ(localZ));
        pos.release();
        return pos;
    }

    /**
     * @author circulation
     * @reason 重写方法以跳过Pos new过程减少内存分配
     */
    @SuppressWarnings("DataFlowIssue")
    @Overwrite
    public IBlockState getBlockState(final int localX, final int y, final int localZ) {
        final int x = this.toWorldX(localX);
        final int z = this.toWorldZ(localZ);
        if (x < 0 || y > 15 || z < 0 || z > 15) {
            Journeymap.getLogger().warn("Expected local coords, got global coords");
        }

        if (!(x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000 && y >= 0 && y < 256)) {
            return Blocks.AIR.getDefaultState();
        } else {
            journeymap.client.model.ChunkMD chunkMD = DataCache.INSTANCE.getChunkMD(ChunkPos.asLong(x >> 4, z >> 4));
            return chunkMD != null && chunkMD.hasChunk() ? chunkMD.getChunk().getBlockState(x & 15, y, z & 15) : Blocks.AIR.getDefaultState();
        }
    }

}
