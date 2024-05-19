package github.kasuminova.stellarcore.mixin.minecraft.renderglobal;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderGlobal.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinRenderGlobal {

    @Redirect(
            method = "updateChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;needsImmediateUpdate()Z"
            ))
    private boolean redirectUpdateChunksNeedsImmediateUpdate(final RenderChunk instance) {
        if (StellarCoreConfig.PERFORMANCE.vanilla.alwaysDeferChunkUpdates) {
            return false;
        }
        return instance.needsImmediateUpdate();
    }

}
