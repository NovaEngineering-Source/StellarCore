package github.kasuminova.stellarcore.mixin.util;

import net.minecraft.block.state.IBlockState;

import java.util.Map;

public interface CachedChunk {

    Map<Short, IBlockState> stellar_core$getBlockStateCache();

    void stellar_core$clearCache();

}
