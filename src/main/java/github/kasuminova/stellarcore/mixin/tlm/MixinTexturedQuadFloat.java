package github.kasuminova.stellarcore.mixin.tlm;

import com.github.tartaricacid.touhoulittlemaid.client.model.TexturedQuadFloat;
import github.kasuminova.stellarcore.client.pool.TLMPositionTextureVertexPool;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.client.model.PositionTextureVertex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(TexturedQuadFloat.class)
public class MixinTexturedQuadFloat {

    @Inject(
            method = "<init>",
            at = @At("RETURN"),
            remap = false
    )
    private void redirectInitSetTexturePosition(final PositionTextureVertex[] vertices, final float texcoordU1, final float texcoordV1, final float texcoordU2, final float texcoordV2, final float textureWidth, final float textureHeight, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.tlm.texturedQuadFloatCanonicalization) {
            return;
        }
        // Canonicalize async
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < vertices.length; i++) {
                vertices[i] = TLMPositionTextureVertexPool.canonicalize(vertices[i]);
            }
        });
    }

}
