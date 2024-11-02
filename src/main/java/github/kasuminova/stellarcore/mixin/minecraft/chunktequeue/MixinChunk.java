package github.kasuminova.stellarcore.mixin.minecraft.chunktequeue;

import github.kasuminova.stellarcore.common.util.FakeConcurrentLinkedQueue;
import github.kasuminova.stellarcore.shaded.org.jctools.queues.MpscUnboundedXaddArrayQueue;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(Chunk.class)
public class MixinChunk {

    @Redirect(
            method = "<init>(Lnet/minecraft/world/World;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Queues;newConcurrentLinkedQueue()Ljava/util/concurrent/ConcurrentLinkedQueue;",
                    remap = false
            )
    )
    private ConcurrentLinkedQueue<BlockPos> redirectNewQueue() {
        // Use new queue type.
        return new FakeConcurrentLinkedQueue<>(new MpscUnboundedXaddArrayQueue<>(16));
    }

}
