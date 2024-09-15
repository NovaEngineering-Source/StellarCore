package github.kasuminova.stellarcore.mixin.tlm;

import com.github.tartaricacid.touhoulittlemaid.client.resources.CustomResourcesLoader;
import github.kasuminova.stellarcore.client.pool.TLMCubesItemPool;
import github.kasuminova.stellarcore.client.pool.TLMPositionTextureVertexPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CustomResourcesLoader.class, remap = false)
public class MixinCustomResourcesLoader {

    @Inject(method = "reloadResources", at = @At("RETURN"))
    private static void injectReloadSources(final CallbackInfo ci) {
        TLMCubesItemPool.clear();
        TLMPositionTextureVertexPool.clear();
    }

}
