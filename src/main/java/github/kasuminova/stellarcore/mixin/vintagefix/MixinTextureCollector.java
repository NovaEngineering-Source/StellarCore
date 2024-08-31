package github.kasuminova.stellarcore.mixin.vintagefix;

import github.kasuminova.stellarcore.client.resource.ResourceExistingCache;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.embeddedt.vintagefix.dynamicresources.TextureCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TextureCollector.class, remap = false)
public class MixinTextureCollector {

    @Inject(method = "startDiscovery", at = @At("HEAD"))
    private static void injectStartDiscovery(final CallbackInfo ci) {
        if (StellarCoreConfig.PERFORMANCE.vanilla.resourceExistStateCache) {
            ResourceExistingCache.enableCache();
        }
    }

}
