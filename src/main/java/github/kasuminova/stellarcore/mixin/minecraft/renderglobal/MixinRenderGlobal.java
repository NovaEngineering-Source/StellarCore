package github.kasuminova.stellarcore.mixin.minecraft.renderglobal;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(RenderGlobal.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinRenderGlobal {

    @Shadow private Set<RenderChunk> chunksToUpdate;

    @Redirect(
            method = "updateChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;needsImmediateUpdate()Z"
            )
    )
    private boolean redirectUpdateChunksNeedsImmediateUpdate(final RenderChunk instance) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.alwaysDeferChunkUpdates) {
            return instance.needsImmediateUpdate();
        }
        return false;
    }

    @Redirect(
            method = "setupTerrain",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;updateChunkNow(Lnet/minecraft/client/renderer/chunk/RenderChunk;)Z"
            )
    )
    private boolean redirectSetupTerrainUpdateChunkNow(final ChunkRenderDispatcher instance, final RenderChunk renderChunk) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.alwaysDeferChunkUpdates) {
            return instance.updateChunkNow(renderChunk);
        }
        this.chunksToUpdate.add(renderChunk);
        return false;
    }

    @Redirect(
            method = "setupTerrain",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;clearNeedsUpdate()V"
            )
    )
    private void redirectSetupTerrainClearNeedsUpdate(final RenderChunk renderChunk) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.alwaysDeferChunkUpdates) {
            renderChunk.clearNeedsUpdate();
        }
    }

}
