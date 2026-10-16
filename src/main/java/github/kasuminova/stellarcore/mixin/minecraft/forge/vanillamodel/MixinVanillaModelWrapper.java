package github.kasuminova.stellarcore.mixin.minecraft.forge.vanillamodel;

import com.google.common.collect.ImmutableMap;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashSet;
import net.minecraftforge.client.model.IModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper", remap = false)
public abstract class MixinVanillaModelWrapper {

    @Unique
    private static final int STELLAR_CORE$VARIANT_CACHE_LIMIT = 8192;

    @Unique
    private NonBlockingHashMap<ImmutableMap<String, String>, IModel> stellar_core$retextureCache;

    /**
     * Variants that have been requested more than once cannot safely share an IModel.
     * Some model implementations mutate the returned model while baking it, so sharing
     * one of these variants can leak state between otherwise unrelated item models.
     */
    @Unique
    private NonBlockingHashSet<ImmutableMap<String, String>> stellar_core$isolatedVariants;

    @Inject(method = "retexture(Lcom/google/common/collect/ImmutableMap;)Lnet/minecraftforge/client/model/ModelLoader$VanillaModelWrapper;", at = @At("HEAD"), remap = false)
    private void stellar_core$retextureFromCache(final ImmutableMap<String, String> textures,
                                                 final CallbackInfoReturnable<IModel> cir) {
        if (!StellarCoreConfig.PERFORMANCE.forge.vanillaModelVariantCache) {
            return;
        }
        final NonBlockingHashSet<ImmutableMap<String, String>> isolated = stellar_core$isolatedVariants;
        if (isolated != null && isolated.contains(textures)) {
            return;
        }
        final NonBlockingHashMap<ImmutableMap<String, String>, IModel> cache = stellar_core$retextureCache;
        if (cache == null) {
            return;
        }
        // A second request for the same variant is evidence that the model may be
        // reused by callers that expect to mutate it independently. Remove the
        // shared instance and isolate this variant from this point onward.
        final IModel cached = cache.remove(textures);
        if (cached != null) {
            stellar_core$markVariantIsolated(textures);
            // Close the race with an in-flight retexture return that may have
            // inserted this variant while isolation was being recorded.
            cache.remove(textures);
        }
    }

    @Inject(method = "retexture(Lcom/google/common/collect/ImmutableMap;)Lnet/minecraftforge/client/model/ModelLoader$VanillaModelWrapper;", at = @At("RETURN"), cancellable = true, remap = false)
    private void stellar_core$retextureToCache(final ImmutableMap<String, String> textures,
                                               final CallbackInfoReturnable<IModel> cir) {
        if (!StellarCoreConfig.PERFORMANCE.forge.vanillaModelVariantCache) {
            return;
        }
        final NonBlockingHashSet<ImmutableMap<String, String>> isolated = stellar_core$isolatedVariants;
        if (isolated != null && isolated.contains(textures)) {
            return;
        }
        NonBlockingHashMap<ImmutableMap<String, String>, IModel> cache = stellar_core$retextureCache;
        if (cache == null) {
            cache = new NonBlockingHashMap<>(4);
            stellar_core$retextureCache = cache;
        }
        if (cache.size() >= STELLAR_CORE$VARIANT_CACHE_LIMIT) {
            return;
        }
        final IModel created = cir.getReturnValue();
        final IModel shared = cache.putIfAbsent(textures, created);
        if (shared != null) {
            cir.setReturnValue(shared);
        }
    }

    @Unique
    private synchronized void stellar_core$markVariantIsolated(final ImmutableMap<String, String> textures) {
        NonBlockingHashSet<ImmutableMap<String, String>> isolated = stellar_core$isolatedVariants;
        if (isolated == null) {
            isolated = new NonBlockingHashSet<>();
            stellar_core$isolatedVariants = isolated;
        }
        isolated.add(textures);
    }

}
