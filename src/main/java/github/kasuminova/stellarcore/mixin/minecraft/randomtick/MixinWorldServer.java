package github.kasuminova.stellarcore.mixin.minecraft.randomtick;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.common.world.ParallelRandomBlockTicker;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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

import java.util.List;

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
        int updateLCG = this.updateLCG;
        ExtendedBlockStorage[] storageArray = chunk.getBlockStorageArray();
        List<ParallelRandomBlockTicker.TickData> tickDataList = new ObjectArrayList<>(storageArray.length + 1);
        for (ExtendedBlockStorage storage : storageArray) {
            if (storage == Chunk.NULL_BLOCK_STORAGE || !storage.needsRandomTick()) {
                continue;
            }

            IntList lcgList = new IntArrayList(tickSpeed + 1);
            for (int i = 0; i < tickSpeed; ++i) {
                updateLCG = (updateLCG * 3) + 0x3c6ef35f;
                int lcg = updateLCG >> 2;
                lcgList.add(lcg);
            }
            tickDataList.add(new ParallelRandomBlockTicker.TickData(storage, lcgList));
        }
        this.updateLCG = updateLCG;
        ParallelRandomBlockTicker.INSTANCE.enqueueChunk(chunk, tickDataList);
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
    private void injectUpdateBlocksEndSelection(final CallbackInfo ci, final @Local(name = "i") int tickSpeed) {
        ParallelRandomBlockTicker.INSTANCE.execute((World) (Object) this, rand, profiler, tickSpeed);
    }

}
