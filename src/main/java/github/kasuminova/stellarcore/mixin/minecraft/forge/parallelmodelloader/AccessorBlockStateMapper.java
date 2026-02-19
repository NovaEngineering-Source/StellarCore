package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(BlockStateMapper.class)
public interface AccessorBlockStateMapper {

    @Accessor("blockStateMap")
    Map<Block, IStateMapper> stellar_core$getBlockStateMap();

}
