package github.kasuminova.stellarcore.mixin.minecraft.chunk_cache;

import github.kasuminova.stellarcore.mixin.util.CachedChunk;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("InstanceofThis")
@Mixin(Chunk.class)
public class MixinChunkClient {

    @Inject(method = "read", at = @At("HEAD"))
    private void injectRead(final PacketBuffer buf, final int availableSections, final boolean groundUpContinuous, final CallbackInfo ci) {
        if (((Object) this) instanceof CachedChunk cachedChunk) {
            cachedChunk.stellar_core$clearCache();
        }
    }

}
