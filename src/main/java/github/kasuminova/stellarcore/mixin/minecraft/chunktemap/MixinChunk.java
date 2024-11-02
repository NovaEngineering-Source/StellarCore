package github.kasuminova.stellarcore.mixin.minecraft.chunktemap;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.BlockPos2ValueMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(Chunk.class)
public class MixinChunk {

    @Final
    @Shadow
    @Mutable
    private Map<BlockPos, TileEntity> tileEntities;

    @Inject(method = "<init>(Lnet/minecraft/world/World;II)V", at = @At("RETURN"))
    private void injectInit(final World worldIn, final int x, final int z, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.chunkTEMap) {
            return;
        }
        this.tileEntities = BlockPos2ValueMap.create();
    }

}
