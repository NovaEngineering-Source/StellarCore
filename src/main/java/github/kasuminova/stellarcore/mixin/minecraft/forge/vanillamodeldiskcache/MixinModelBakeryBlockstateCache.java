package github.kasuminova.stellarcore.mixin.minecraft.forge.vanillamodeldiskcache;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.stellarcore.client.model.vanillacache.BlockstateCapture;
import github.kasuminova.stellarcore.client.model.vanillacache.TeeingResource;
import github.kasuminova.stellarcore.client.model.vanillacache.VanillaModelDiskCache;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ModelBakery.class)
public abstract class MixinModelBakeryBlockstateCache {

    @Inject(method = "loadMultipartMBD", at = @At("HEAD"), cancellable = true)
    private void stellar_core$serveFromDiskCache(final ResourceLocation location,
                                                 final ResourceLocation fileIn,
                                                 final CallbackInfoReturnable<ModelBlockDefinition> cir) {
        final VanillaModelDiskCache cache = VanillaModelDiskCache.INSTANCE;
        if (!cache.isEnabled()) {
            return;
        }
        try {
            final ModelBlockDefinition cached = cache.tryBuildBlockstate(fileIn, location);
            if (cached != null) {
                cir.setReturnValue(cached);
                return;
            }
        } catch (Throwable ignored) {
            // The disk cache must never break the load pipeline.
        }
        BlockstateCapture.begin();
    }

    @WrapOperation(
            method = "loadMultipartMBD",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/model/ModelBakery;loadModelBlockDefinition(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/resources/IResource;)Lnet/minecraft/client/renderer/block/model/ModelBlockDefinition;"
            )
    )
    private ModelBlockDefinition stellar_core$capturePart(final ModelBakery bakery,
                                                          final ResourceLocation location,
                                                          final IResource resource,
                                                          final Operation<ModelBlockDefinition> original) {
        final VanillaModelDiskCache cache = VanillaModelDiskCache.INSTANCE;
        if (!cache.isEnabled()) {
            return original.call(bakery, location, resource);
        }
        try {
            return original.call(bakery, location, new TeeingResource(resource));
        } catch (Throwable t) {
            BlockstateCapture.discard();
            throw t;
        }
    }

    @Inject(method = "loadMultipartMBD", at = @At("RETURN"))
    private void stellar_core$storeCaptured(final ResourceLocation location,
                                            final ResourceLocation fileIn,
                                            final CallbackInfoReturnable<ModelBlockDefinition> cir) {
        final byte[][] parts = BlockstateCapture.collect();
        if (parts == null) {
            return;
        }
        final VanillaModelDiskCache cache = VanillaModelDiskCache.INSTANCE;
        if (!cache.isEnabled()) {
            return;
        }
        try {
            cache.captureBlockstate(fileIn, parts);
        } catch (Throwable ignored) {
        }
    }
}
