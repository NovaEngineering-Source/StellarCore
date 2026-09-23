package github.kasuminova.stellarcore.mixin.minecraft.forge.objmodel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import github.kasuminova.stellarcore.client.model.obj.OBJBakeCacheKey;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.Models;
import net.minecraftforge.common.model.TRSRTransformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Mixin(value = OBJModel.class, remap = false)
public class MixinOBJModel {

    @Unique
    private static final int STELLAR_CORE$VARIANT_CACHE_LIMIT = 8192;

    @Shadow
    private OBJModel.MaterialLibrary matLib;

    @Unique
    private NonBlockingHashMap<ImmutableMap<String, String>, IModel> stellar_core$processCache;

    @Unique
    private NonBlockingHashMap<ImmutableMap<String, String>, IModel> stellar_core$retextureCache;

    @Unique
    private NonBlockingHashMap<OBJBakeCacheKey, IBakedModel> stellar_core$bakeCache;

    @Inject(method = "process", at = @At("HEAD"), cancellable = true, remap = false)
    private void stellar_core$processFromCache(final ImmutableMap<String, String> customData,
                                               final CallbackInfoReturnable<IModel> cir) {
        if (!StellarCoreConfig.PERFORMANCE.forge.objModelVariantCache) {
            return;
        }
        final NonBlockingHashMap<ImmutableMap<String, String>, IModel> cache = stellar_core$processCache;
        if (cache == null) {
            return;
        }
        final IModel cached = cache.get(customData);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "process", at = @At("RETURN"), cancellable = true, remap = false)
    private void stellar_core$processToCache(final ImmutableMap<String, String> customData,
                                             final CallbackInfoReturnable<IModel> cir) {
        if (!StellarCoreConfig.PERFORMANCE.forge.objModelVariantCache) {
            return;
        }
        NonBlockingHashMap<ImmutableMap<String, String>, IModel> cache = stellar_core$processCache;
        if (cache == null) {
            cache = new NonBlockingHashMap<>(4);
            stellar_core$processCache = cache;
        }
        if (cache.size() >= STELLAR_CORE$VARIANT_CACHE_LIMIT) {
            return;
        }
        final IModel created = cir.getReturnValue();
        final IModel shared = cache.putIfAbsent(customData, created);
        if (shared != null) {
            cir.setReturnValue(shared);
        }
    }

    @Inject(method = "retexture", at = @At("HEAD"), cancellable = true, remap = false)
    private void stellar_core$retextureFromCache(final ImmutableMap<String, String> textures,
                                                 final CallbackInfoReturnable<IModel> cir) {
        if (!StellarCoreConfig.PERFORMANCE.forge.objModelVariantCache) {
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

    @Inject(method = "retexture", at = @At("RETURN"), cancellable = true, remap = false)
    private void stellar_core$retextureToCache(final ImmutableMap<String, String> textures,
                                               final CallbackInfoReturnable<IModel> cir) {
        if (!StellarCoreConfig.PERFORMANCE.forge.objModelVariantCache) {
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

    @Inject(method = "bake", at = @At("HEAD"), cancellable = true, remap = false)
    private void stellar_core$bakeFromCache(final IModelState state, final VertexFormat format,
                                            final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
                                            final CallbackInfoReturnable<IBakedModel> cir) {
        if (!StellarCoreConfig.PERFORMANCE.forge.objModelBakeCache) {
            return;
        }
        final NonBlockingHashMap<OBJBakeCacheKey, IBakedModel> cache = stellar_core$bakeCache;
        if (cache == null) {
            return;
        }
        final OBJBakeCacheKey key = stellar_core$bakeKey(state, format);
        if (key == null) {
            return;
        }
        final IBakedModel cached = cache.get(key);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "bake", at = @At("RETURN"), cancellable = true, remap = false)
    private void stellar_core$bakeToCache(final IModelState state, final VertexFormat format,
                                          final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
                                          final CallbackInfoReturnable<IBakedModel> cir) {
        if (!StellarCoreConfig.PERFORMANCE.forge.objModelBakeCache) {
            return;
        }
        final OBJBakeCacheKey key = stellar_core$bakeKey(state, format);
        if (key == null) {
            return;
        }
        NonBlockingHashMap<OBJBakeCacheKey, IBakedModel> cache = stellar_core$bakeCache;
        if (cache == null) {
            cache = new NonBlockingHashMap<>(4);
            stellar_core$bakeCache = cache;
        }
        if (cache.size() >= STELLAR_CORE$VARIANT_CACHE_LIMIT) {
            return;
        }
        final IBakedModel created = cir.getReturnValue();
        final IBakedModel shared = cache.putIfAbsent(key, created);
        if (shared != null) {
            cir.setReturnValue(shared);
        }
    }

    @Unique
    private OBJBakeCacheKey stellar_core$bakeKey(final IModelState state, final VertexFormat format) {
        if (state == null || state instanceof OBJModel.OBJState) {
            return null;
        }
        final Map<String, OBJModel.Group> groups = this.matLib.getGroups();
        if (groups.size() > Long.SIZE) {
            return null;
        }
        long visibility = 0L;
        int index = 0;
        for (final String name : groups.keySet()) {
            if (!state.apply(Optional.of(Models.getHiddenModelPart(ImmutableList.of(name)))).isPresent()) {
                visibility |= 1L << index;
            }
            index++;
        }
        final Optional<TRSRTransformation> transform = state.apply(Optional.empty());
        return new OBJBakeCacheKey(format, transform.orElse(null), visibility);
    }

}
