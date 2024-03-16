package github.kasuminova.stellarcore.mixin.minecraft;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.BlockPos2ValueMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = Chunk.class, priority = 1001)
public class MixinChunk {

    @Shadow @Final @Mutable private Map<BlockPos, TileEntity> tileEntities;

    @Redirect(method = "<init>(Lnet/minecraft/world/World;II)V", at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/Chunk;tileEntities:Ljava/util/Map;"))
    private void redirectNewHashMap(final Chunk instance, final Map<BlockPos, TileEntity> value) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.blockPos2ValueMap) {
            return;
        }
        this.tileEntities = new BlockPos2ValueMap<>();
    }

}
