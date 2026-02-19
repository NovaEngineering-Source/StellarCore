package github.kasuminova.stellarcore.mixin.minecraft.stitcher;

import github.kasuminova.stellarcore.client.texture.StitcherCache;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.renderer.texture.ITextureMapPopulator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(TextureMap.class)
public class MixinTextureMap {

    @Final
    @Shadow
    @Mutable
    private Map<String, TextureAtlasSprite> mapRegisteredSprites;

    @Final
    @Shadow
    @Mutable
    private Map<String, TextureAtlasSprite> mapUploadedSprites;

    @Inject(method = "<init>(Ljava/lang/String;Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;Z)V", at = @At("RETURN"))
    private void injectInit(final String basePathIn, final ITextureMapPopulator iconCreatorIn, final boolean skipFirst, final CallbackInfo ci) {
        this.mapUploadedSprites = new Object2ObjectOpenHashMap<>();

        // When model loading is parallelized, some mods may register sprites from multiple
        // threads. Vanilla uses HashMap which is not thread-safe and can randomly drop entries.
        // Use a concurrent map to keep sprite registration deterministic.
        if (StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader || StellarCoreConfig.PERFORMANCE.vanilla.parallelTextureLoad) {
            this.mapRegisteredSprites = new ConcurrentHashMap<>();
        } else {
            this.mapRegisteredSprites = new Object2ObjectOpenHashMap<>();
        }
    }

    @Inject(method = "loadSprites", at = @At("HEAD"))
    private void injectLoadSpritesResetTracker(final IResourceManager resourceManager, final ITextureMapPopulator iconCreatorIn, final CallbackInfo ci) {
        // Ensure StitcherCache can resolve the correct cache on reloads.
        final TextureMap self = (TextureMap) (Object) this;
        if (StitcherCache.hasCacheFor(self)) {
            StitcherCache.setActiveMap(self);
        }
    }

    /**
     * After mods have registered their sprites, pre-fill any sprites that exist in
     * the stitcher cache but were not registered this run. This stabilizes the stitched
     * set across launches despite nondeterministic mod registration (e.g. mods that
     * randomly select a subset of icons each launch).
     */
    @Inject(
            method = "loadSprites",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;registerSprites(Lnet/minecraft/client/renderer/texture/TextureMap;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void injectLoadSpritesAfterRegisterSpritesFillFromCache(final IResourceManager resourceManager, final ITextureMapPopulator iconCreatorIn, final CallbackInfo ci) {
        if (!(StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader || StellarCoreConfig.PERFORMANCE.vanilla.parallelTextureLoad)) {
            return;
        }

        final TextureMap self = (TextureMap) (Object) this;
        final StitcherCache cache = StitcherCache.getCacheFor(self);
        if (cache == null) {
            return;
        }

        final Set<String> cachedSpriteNames = cache.getCachedSpriteNamesFromFile();
        if (cachedSpriteNames.isEmpty()) {
            return;
        }

        int filled = 0;
        for (final String spriteName : cachedSpriteNames) {
            if (spriteName == null || spriteName.isEmpty() || spriteName.indexOf(':') < 0) {
                continue;
            }
            if (mapRegisteredSprites.containsKey(spriteName)) {
                continue;
            }
            try {
                self.registerSprite(new ResourceLocation(spriteName));
                filled++;
            } catch (Throwable ignored) {
            }
        }

        if (filled > 0) {
            StellarLog.LOG.info(
                    "[StellarCore-MixinTextureMap] Pre-registered {} sprites from stitcher cache for atlas `{}`.",
                    filled, self.getBasePath()
            );
        }
    }

}
