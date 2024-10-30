package github.kasuminova.stellarcore.mixin.minecraft.chunktequeue;

import github.kasuminova.stellarcore.shaded.org.jctools.queues.MpscUnboundedXaddArrayQueue;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(Chunk.class)
public class MixinChunk {

    @Unique
    private final Queue<BlockPos> stellar_core$tileEntityPosQueue = new MpscUnboundedXaddArrayQueue<>(16);

    @Redirect(
            method = "<init>(Lnet/minecraft/world/World;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Queues;newConcurrentLinkedQueue()Ljava/util/concurrent/ConcurrentLinkedQueue;",
                    remap = false
            )
    )
    private ConcurrentLinkedQueue<BlockPos> redirectNewQueue() {
        // Deallocate the queue.
        return null;
    }

    @Redirect(
            method = "getTileEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/ConcurrentLinkedQueue;add(Ljava/lang/Object;)Z",
                    remap = false
            )
    )
    private boolean redirectGetTileEntityQueueAdd(final ConcurrentLinkedQueue<BlockPos> _inst, final Object element) {
        return stellar_core$tileEntityPosQueue.add((BlockPos) element);
    }

    @Redirect(
            method = "onTick",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/ConcurrentLinkedQueue;isEmpty()Z",
                    remap = false
            )
    )
    private boolean redirectOnTickQueueIsEmpty(final ConcurrentLinkedQueue<BlockPos> _inst) {
        return stellar_core$tileEntityPosQueue.isEmpty();
    }

    @Redirect(
            method = "onTick",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/ConcurrentLinkedQueue;poll()Ljava/lang/Object;",
                    remap = false
            )
    )
    private Object redirectOnTickQueuePoll(final ConcurrentLinkedQueue<BlockPos> _inst) {
        return stellar_core$tileEntityPosQueue.poll();
    }

}
