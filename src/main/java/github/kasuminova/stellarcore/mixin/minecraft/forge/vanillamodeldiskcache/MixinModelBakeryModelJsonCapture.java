package github.kasuminova.stellarcore.mixin.minecraft.forge.vanillamodeldiskcache;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.stellarcore.client.model.vanillacache.ModelCapture;
import github.kasuminova.stellarcore.client.model.vanillacache.TeeingResource;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Copies the model JSON the vanilla loader parses into the current
 * {@link ModelCapture} scope.
 *
 * <p>The cache stores the resource the loader actually read, rather than reading
 * the same location a second time afterwards: the second read costs a full
 * resource lookup per model on the critical path, and when it comes back empty
 * the cache silently stays empty forever. See {@link ModelCapture}.</p>
 *
 * <p>This is the only injector in its class on purpose. A tap is an
 * optimisation, so if the call site is ever missing the cache must lose the tap
 * and nothing else — the capture injectors live in
 * {@link MixinModelBakeryVanillaCache}, and they fall back to reading the file
 * themselves.</p>
 */
@Mixin(ModelBakery.class)
public abstract class MixinModelBakeryModelJsonCapture {

    @WrapOperation(
            method = "loadModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/IResourceManager;getResource(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/resources/IResource;"
            )
    )
    private IResource stellar_core$tapModelJson(final IResourceManager manager,
                                                final ResourceLocation location,
                                                final Operation<IResource> original) {
        final IResource resource = original.call(manager, location);
        if (!ModelCapture.isActive()) {
            return resource;
        }
        return new TeeingResource(resource, ModelCapture::teeModelJson);
    }
}
