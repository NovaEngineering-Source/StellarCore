package github.kasuminova.stellarcore.mixin.minecraft.randomtick;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.common.world.ParallelRandomBlockTicker;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer extends World {

    @Unique
    private static final ExtendedBlockStorage[] EMPTY_ARRAY = new ExtendedBlockStorage[0];

    @SuppressWarnings("DataFlowIssue")
    protected MixinWorldServer() {
        super(null, null, null, null, false);
    }

    @Redirect(
            method = "updateBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/Chunk;getBlockStorageArray()[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;"
            )
    )
    private ExtendedBlockStorage[] redirectUpdateBlocksGetBlockStorageArray(final Chunk chunk, final @Local(name = "i") int tickSpeed) {
        final ExtendedBlockStorage[] storageArray = chunk.getBlockStorageArray();

        // Vanilla advances the LCG only inside the branch that needs a tick, so a chunk that has nothing to tick can
        // return here without touching the world state, and without the bookkeeping it used to do for every chunk.
        boolean anyTickable = false;
        for (final ExtendedBlockStorage storage : storageArray) {
            if (storage != Chunk.NULL_BLOCK_STORAGE && storage.needsRandomTick()) {
                anyTickable = true;
                break;
            }
        }
        if (!anyTickable) {
            return EMPTY_ARRAY;
        }

        final ParallelRandomBlockTicker ticker = ParallelRandomBlockTicker.INSTANCE;
        final int chunkX = chunk.x;
        final int chunkZ = chunk.z;
        int updateLCG = this.updateLCG;
        for (final ExtendedBlockStorage storage : storageArray) {
            if (storage == Chunk.NULL_BLOCK_STORAGE || !storage.needsRandomTick()) {
                continue;
            }
            updateLCG = ticker.enqueueSection(storage, chunkX, chunkZ, tickSpeed, updateLCG);
        }
        this.updateLCG = updateLCG;
        return EMPTY_ARRAY;
    }

    @Inject(
            method = "updateBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/profiler/Profiler;endSection()V",
                    ordinal = 2
            )
    )
    @SuppressWarnings("RedundantCast")
    private void injectUpdateBlocksEndSelection(final CallbackInfo ci) {
        ParallelRandomBlockTicker.INSTANCE.execute((World) (Object) this, rand, profiler);
    }

}
