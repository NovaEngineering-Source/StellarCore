package github.kasuminova.stellarcore.mixin.tconstruct;

import com.google.gson.JsonParseException;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.client.CustomTextureCreator;
import slimeknights.tconstruct.library.client.model.ModelHelper;
import slimeknights.tconstruct.library.client.model.ModifierModel;
import slimeknights.tconstruct.library.client.model.ModifierModelLoader;
import slimeknights.tconstruct.library.modifiers.IModifier;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mixin(value = ModifierModelLoader.class, remap = false)
public class MixinModifierModelLoader {

    @Shadow
    @Final
    private static Logger log;

    @Shadow
    protected Map<String, List<ResourceLocation>> locations;

    @Shadow
    protected Map<String, Map<String, String>> cache;

    @Unique
    private volatile boolean stellar_core$cacheLoaded;

    @Inject(method = "loadModel", at = @At("HEAD"), cancellable = true, remap = false)
    private void stellar_core$loadModel(final ResourceLocation modelLocation, final CallbackInfoReturnable<IModel> cir) {
        if (!stellar_core$concurrent()) {
            return;
        }
        cir.setReturnValue(stellar_core$loadModelConcurrent(modelLocation));
    }

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"), remap = false)
    private void stellar_core$onResourceManagerReload(final IResourceManager resourceManager, final CallbackInfo ci) {
        if (!stellar_core$concurrent()) {
            return;
        }
        synchronized (this) {
            stellar_core$cacheLoaded = false;
        }
    }

    @Unique
    private IModel stellar_core$loadModelConcurrent(final ResourceLocation modelLocation) {
        final String toolname = FilenameUtils.getBaseName(modelLocation.getPath()).toLowerCase(Locale.US);
        final Map<String, Map<String, String>> modelCache = stellar_core$ensureCache();

        final String location = modelLocation.getPath().substring(17);
        final ResourceLocation toolModifiers = new ResourceLocation(modelLocation.getNamespace(), "models/item/" + location);

        try {
            final Map<String, String> textureEntries = ModelHelper.loadTexturesFromJson(toolModifiers);
            final Map<String, String> toolCache = modelCache.computeIfAbsent(toolname, key -> new NonBlockingHashMap<>());
            for (final Map.Entry<String, String> textureEntry : textureEntries.entrySet()) {
                final String modifier = textureEntry.getKey().toLowerCase(Locale.US);
                toolCache.put(modifier, textureEntry.getValue());
            }
        } catch (IOException e) {
            log.debug("No tool modifier model found at {}, skipping", toolModifiers);
        } catch (JsonParseException e) {
            log.error("Cannot load tool modifier-model for {}", toolModifiers, e);
            throw e;
        }

        final ModifierModel model = new ModifierModel();
        final Map<String, String> cachedTool = modelCache.get(toolname);
        if (cachedTool != null) {
            for (final Map.Entry<String, String> entry : cachedTool.entrySet()) {
                final IModifier mod = TinkerRegistry.getModifier(entry.getKey());
                model.addModelForModifier(entry.getKey(), entry.getValue());
                if (mod != null && mod.hasTexturePerMaterial()) {
                    CustomTextureCreator.registerTexture(new ResourceLocation(entry.getValue()));
                }
            }
        } else {
            log.debug("Tried to load modifier models for {}but none were found", toolname);
        }
        return model;
    }

    @Unique
    private Map<String, Map<String, String>> stellar_core$ensureCache() {
        if (!stellar_core$cacheLoaded) {
            synchronized (this) {
                if (!stellar_core$cacheLoaded) {
                    final Map<String, Map<String, String>> loaded = new NonBlockingHashMap<>();
                    stellar_core$loadFilesIntoCache(loaded);
                    this.cache = loaded;
                    stellar_core$cacheLoaded = true;
                }
            }
        }
        return this.cache;
    }

    @Unique
    private void stellar_core$loadFilesIntoCache(final Map<String, Map<String, String>> target) {
        target.put("default", new NonBlockingHashMap<>());

        for (final Map.Entry<String, List<ResourceLocation>> entry : this.locations.entrySet()) {
            final String modifier = entry.getKey();

            for (final ResourceLocation location : entry.getValue()) {
                try {
                    final Map<String, String> textureEntries = ModelHelper.loadTexturesFromJson(location);
                    for (final Map.Entry<String, String> textureEntry : textureEntries.entrySet()) {
                        final String tool = textureEntry.getKey().toLowerCase(Locale.US);
                        final Map<String, String> toolCache = target.computeIfAbsent(tool, key -> new NonBlockingHashMap<>());
                        if (!toolCache.containsKey(modifier)) {
                            toolCache.put(modifier, textureEntry.getValue());
                        }
                    }
                } catch (IOException e) {
                    log.error("Cannot load modifier-model {}", location, e);
                } catch (JsonParseException e) {
                    log.error("Cannot load modifier-model {}", location, e);
                    throw e;
                }
            }

            if (!target.get("default").containsKey(modifier)) {
                log.debug("{} Modifiers model does not contain a default-entry", modifier);
            }
        }

        final Map<String, String> defaults = target.get("default");
        for (final Map.Entry<String, Map<String, String>> toolEntry : target.entrySet()) {
            final Map<String, String> textures = toolEntry.getValue();
            for (final Map.Entry<String, String> defaultEntry : defaults.entrySet()) {
                if (!textures.containsKey(defaultEntry.getKey())) {
                    log.debug("Filling in default for modifier {} on tool {}", defaultEntry.getKey(), toolEntry.getKey());
                    textures.put(defaultEntry.getKey(), defaultEntry.getValue());
                }
            }
        }
    }

    @Unique
    private static boolean stellar_core$concurrent() {
        return StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader;
    }

}
