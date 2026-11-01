package github.kasuminova.stellarcore.mixin.libnine;

import com.google.gson.JsonObject;
import github.kasuminova.stellarcore.client.integration.libnine.L9ModScanner;
import github.kasuminova.stellarcore.client.resource.ResourceExistingCache;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import io.github.phantamanta44.libnine.client.model.L9Models;
import io.github.phantamanta44.libnine.util.helper.ResourceUtils;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = L9Models.class, remap = false)
public class MixinL9Models implements StellarCoreResourcePack {

    @Unique
    private static final Object stellar_core$NO_MODEL_TYPE = new Object();

    @Unique
    private static volatile Map<ResourceLocation, Object> stellar_core$validTypeCache = null;

    @SuppressWarnings("RedundantCast")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.resourceExistStateCache) {
            return;
        }
        ResourceExistingCache.addResourcePack((StellarCoreResourcePack) (Object) this);
    }

    @Inject(method = "isOfType", at = @At("HEAD"), cancellable = true)
    private static void injectIsOfType(final ResourceLocation resource, final String type, final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.resourceExistStateCache) {
            return;
        }
        if (!StellarCoreConfig.PERFORMANCE.libNine.l9ModelsIsOfTypeCache) {
            return;
        }

        // Skip non-libnine mods
        if (!L9ModScanner.isValidMod(resource.getNamespace())) {
            cir.setReturnValue(Boolean.FALSE);
        }

        final Object modelType = stellar_core$getValidTypeCache().computeIfAbsent(
                resource, MixinL9Models::stellar_core$readModelType);
        cir.setReturnValue(modelType != stellar_core$NO_MODEL_TYPE && modelType.equals(type));
    }

    @Unique
    private static Object stellar_core$readModelType(final ResourceLocation resource) {
        try {
            final JsonObject model = ResourceUtils.getAsJson(resource).getAsJsonObject();
            return model.has("9s") ? model.get("9s").getAsString() : stellar_core$NO_MODEL_TYPE;
        } catch (Exception ignored) {
            return stellar_core$NO_MODEL_TYPE;
        }
    }

    @Override
    public void stellar_core$onReload() {
        stellar_core$getValidTypeCache().clear();
    }

    @Override
    public void stellar_core$disableCache() {
        stellar_core$getValidTypeCache().clear();
    }

    @Override
    public void stellar_core$enableCache() {
        stellar_core$getValidTypeCache().clear();
    }

    @Override
    public boolean stellar_core$isPersistent() {
        return true;
    }

    @Unique
    private static Map<ResourceLocation, Object> stellar_core$getValidTypeCache() {
        if (stellar_core$validTypeCache == null) {
            synchronized (L9Models.class) {
                if (stellar_core$validTypeCache == null) {
                    stellar_core$validTypeCache = new NonBlockingHashMap<>();
                }
            }
        }
        return stellar_core$validTypeCache;
    }

}
