package dev.tr7zw.entityculling;

import com.logisticscraft.occlusionculling.DataProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DefaultChunkDataProvider implements DataProvider {
    private final World level;

    public DefaultChunkDataProvider(World level) {
        this.level = level;
    }

    @Override
    public boolean prepareChunk(int chunkX, int chunkZ) {
        return this.level.getChunkProvider().getLoadedChunk(chunkX, chunkZ) != null;
    }

    @Override
    public boolean isOpaqueFullCube(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = level.getBlockState(pos);
        return state.isOpaqueCube();
    }

    @Override
    public void cleanup() {
        DataProvider.super.cleanup();
    }

}