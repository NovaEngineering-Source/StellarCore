package github.kasuminova.stellarcore.mixin.libnine;

import com.google.gson.stream.JsonReader;
import github.kasuminova.stellarcore.client.integration.libnine.L9ModScanner;
import github.kasuminova.stellarcore.client.integration.libnine.L9ModelTypeReader;
import github.kasuminova.stellarcore.client.resource.ResourceExistingCache;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import io.github.phantamanta44.libnine.client.model.L9Models;
import io.github.phantamanta44.libnine.util.helper.ResourceUtils;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = L9Models.class, remap = false)
public class MixinL9Models implements StellarCoreResourcePack {

    @Unique
    private static final Object stellar_core$NO_MODEL_TYPE = new Object();

    @Unique
    private static volatile Map<ResourceLocation, Object> stellar_core$validTypeCache = null;

    @SuppressWarnings("RedundantCast")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        ResourceExistingCache.addResourcePack((StellarCoreResourcePack) (Object) this);
    }

    /**
     * @author StellarCore
     * @reason Cache model type only inside the resource-loading lifecycle.
     *
     */
    @Overwrite
    public static boolean isOfType(final ResourceLocation resource, final String type) {
        final Map<ResourceLocation, Object> cache = stellar_core$validTypeCache;
        if (cache == null) {
            final Object modelType = stellar_core$readModelType(resource);
            return modelType != stellar_core$NO_MODEL_TYPE && modelType.equals(type);
        }

        if (!L9ModScanner.isValidMod(resource.getNamespace())) {
            return false;
        }

        final Object modelType = cache.computeIfAbsent(
            resource, MixinL9Models::stellar_core$readModelType);
        return modelType != stellar_core$NO_MODEL_TYPE && modelType.equals(type);
    }

    @Unique
    private static Object stellar_core$readModelType(final ResourceLocation resource) {
        try (JsonReader reader = new JsonReader(new InputStreamReader(ResourceUtils.getResource(resource).getInputStream()))) {
            final String type = L9ModelTypeReader.read(reader);
            return type != null ? type : stellar_core$NO_MODEL_TYPE;
        } catch (Exception ignored) {
            return stellar_core$NO_MODEL_TYPE;
        }
    }

    @Override
    public void stellar_core$onReload() {
        if (stellar_core$validTypeCache != null) {
            stellar_core$validTypeCache = new ConcurrentHashMap<>();
        }
    }

    @Override
    public void stellar_core$disableCache() {
        stellar_core$validTypeCache = null;
    }

    @Override
    public void stellar_core$enableCache() {
        stellar_core$validTypeCache = new ConcurrentHashMap<>();
    }

    @Override
    public boolean stellar_core$isPersistent() {
        return true;
    }

}
