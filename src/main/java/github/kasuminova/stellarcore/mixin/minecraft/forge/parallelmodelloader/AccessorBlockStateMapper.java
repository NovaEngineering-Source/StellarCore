package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(BlockStateMapper.class)
public interface AccessorBlockStateMapper {

    @Accessor
    Map<IBlockState, ModelResourceLocation> getBlockStateMap();

}
