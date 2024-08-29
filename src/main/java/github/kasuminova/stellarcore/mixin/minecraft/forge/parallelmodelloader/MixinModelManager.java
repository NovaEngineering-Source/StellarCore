package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import github.kasuminova.stellarcore.client.model.ModelLoaderRegistryRef;
import github.kasuminova.stellarcore.client.resource.ResourceExistingCache;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(ModelManager.class)
public class MixinModelManager {

    @Inject(method = "onResourceManagerReload", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/model/ModelLoader;setupModelRegistry()Lnet/minecraft/util/registry/IRegistry;"))
    private void injectBefore(final IResourceManager resourceManager, final CallbackInfo ci) {
        ModelLoaderRegistryRef.instance.stellar_core$toConcurrent();
        ResourceExistingCache.enableCache();
    }

    @Inject(method = "onResourceManagerReload",
            at = @At(
                    value = "INVOKE", 
                    target = "Lnet/minecraftforge/client/ForgeHooksClient;onModelBake(Lnet/minecraft/client/renderer/block/model/ModelManager;Lnet/minecraft/util/registry/IRegistry;Lnet/minecraftforge/client/model/ModelLoader;)V", 
                    remap = false
            )
    )
    private void injectBeforeBake(final IResourceManager resourceManager, final CallbackInfo ci) {
        ModelLoaderRegistryRef.instance.stellar_core$writeToOriginalMap();
    }

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    private void injectAfter(final IResourceManager resourceManager, final CallbackInfo ci) {
        ModelLoaderRegistryRef.instance.stellar_core$toDefault();
        ResourceExistingCache.disableCache();
    }

}
