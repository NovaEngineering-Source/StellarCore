package github.kasuminova.stellarcore.mixin.draconicevolution;

import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyPylon;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(TileEnergyPylon.class)
public class MixinTileEnergyPylon {

    /**
     * Prevent chunk load.
     */
    @Redirect(
            method = "getCore",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getChunk(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/chunk/Chunk;"
            ),
            remap = false
    )
    private Chunk redirectGetCore(final World instance, final BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        return instance.getChunkProvider().getLoadedChunk(chunkPos.x, chunkPos.z);
    }

}
