package github.kasuminova.stellarcore.mixin.minecraft.forge.vanillamodeldiskcache;

import github.kasuminova.stellarcore.client.model.vanillacache.VanillaModelDiskCache;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelBakery.class)
public abstract class MixinModelBakeryVanillaCache {

    @Final
    @Shadow
    protected IResourceManager resourceManager;

    /**
     * How long the model loader waits for the background cache pipeline before
     * giving up on it. The pipeline starts back in construction() and normally
     * finishes long before models are needed, so this is almost always zero. When
     * it has not finished, the remaining work is one file read, which is still far
     * cheaper than loading thousands of models the slow way.
     */
    @Unique
    private static final long AWAIT_PREPARED_MILLIS = 5000L;

    @Inject(method = "loadModel", at = @At("HEAD"), cancellable = true)
    private void stellar_core$serveFromDiskCache(final ResourceLocation location,
                                                 final CallbackInfoReturnable<ModelBlock> cir) {
        final VanillaModelDiskCache cache = VanillaModelDiskCache.INSTANCE;
        if (!cache.isEnabled() || stellarCore$isBuiltin(location)) {
            return;
        }
        try {
            // Only the first model can block here; once the pipeline is done this
            // is just an isDone() check.
            if (!cache.isPrepared()) {
                cache.awaitPrepared(AWAIT_PREPARED_MILLIS);
            }
            final ModelBlock cached = cache.tryBuildModelBlock(location);
            if (cached != null) {
                cir.setReturnValue(cached);
            }
        } catch (Throwable ignored) {
            // The disk cache must never break the load pipeline.
        }
    }

    @Inject(method = "loadModel", at = @At("RETURN"))
    private void stellar_core$captureSnapshot(final ResourceLocation location,
                                              final CallbackInfoReturnable<ModelBlock> cir) {
        final VanillaModelDiskCache cache = VanillaModelDiskCache.INSTANCE;
        if (!cache.isEnabled() || stellarCore$isBuiltin(location)) {
            return;
        }
        if (cir.getReturnValue() == null) {
            return;
        }
        if (cache.knows(location)) {
            return;
        }
        try {
            cache.captureFrom(location, resourceManager);
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static boolean stellarCore$isBuiltin(final ResourceLocation location) {
        return location.getPath().startsWith("builtin/");
    }
}
