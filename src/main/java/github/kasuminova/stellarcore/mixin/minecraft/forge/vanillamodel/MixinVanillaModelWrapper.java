package github.kasuminova.stellarcore.mixin.minecraft.forge.vanillamodel;

import com.google.common.collect.ImmutableMap;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
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

    @Inject(method = "retexture(Lcom/google/common/collect/ImmutableMap;)Lnet/minecraftforge/client/model/ModelLoader$VanillaModelWrapper;", at = @At("HEAD"), cancellable = true, remap = false)
    private void stellar_core$retextureFromCache(final ImmutableMap<String, String> textures,
                                                 final CallbackInfoReturnable<IModel> cir) {
        if (!StellarCoreConfig.PERFORMANCE.forge.vanillaModelVariantCache) {
            return;
        }
        final NonBlockingHashMap<ImmutableMap<String, String>, IModel> cache = stellar_core$retextureCache;
        if (cache == null) {
            return;
        }
        final IModel cached = cache.get(textures);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "retexture(Lcom/google/common/collect/ImmutableMap;)Lnet/minecraftforge/client/model/ModelLoader$VanillaModelWrapper;", at = @At("RETURN"), cancellable = true, remap = false)
    private void stellar_core$retextureToCache(final ImmutableMap<String, String> textures,
                                               final CallbackInfoReturnable<IModel> cir) {
        if (!StellarCoreConfig.PERFORMANCE.forge.vanillaModelVariantCache) {
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

}
