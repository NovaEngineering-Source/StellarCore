package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import github.kasuminova.stellarcore.client.model.ModelLoaderRegistryRef;
import github.kasuminova.stellarcore.client.resource.ResourceExistingCache;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.StellarCoreStateMapper;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(ModelManager.class)
public class MixinModelManager {

    @Final
    @Shadow
    private BlockModelShapes modelProvider;

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
        if (StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader) {
            for (final IStateMapper stateMapper : ((AccessorBlockStateMapper) this.modelProvider.getBlockStateMapper())
                .stellar_core$getBlockStateMap().values()) {
                if (stateMapper instanceof StellarCoreStateMapper) {
                    ((StellarCoreStateMapper) stateMapper).stellar_core$freeze();
                }
            }
        }
    }

}
