package github.kasuminova.stellarcore.mixin.minecraft.texture_load;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.client.texture.SpriteBufferedImageCache;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(TextureMap.class)
public abstract class MixinTextureMap {

    @Final
    @Shadow
    private Map<String, TextureAtlasSprite> mapRegisteredSprites;

    @Shadow
    protected abstract ResourceLocation getResourceLocation(final TextureAtlasSprite p_184396_1_);

    @Shadow
    private int mipmapLevels;

    @Unique
    private int stellar_core$prevMipMapLevels;

    @Unique
    private Set<TextureAtlasSprite> stellar_core$cachedTextures;

    @Unique
    private Set<ResourceLocation> stellar_core$cachedLocations;

    @Inject(method = "loadSprites", 
            at = @At(
                    value = "INVOKE", 
                    target = "Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;registerSprites(Lnet/minecraft/client/renderer/texture/TextureMap;)V", 
                    shift = At.Shift.AFTER
            )
    )
    private void injectLoadSpritesAfter(final IResourceManager resourceManager, final ITextureMapPopulator iconCreatorIn, final CallbackInfo ci) {
        SpriteBufferedImageCache.INSTANCE.clear();
        stellar_core$cachedTextures = java.util.concurrent.ConcurrentHashMap.newKeySet();
        stellar_core$cachedLocations = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Future<Integer> detectMaxMipmapLevelTask = stellar_core$initializeOptifineTask(resourceManager);
        mapRegisteredSprites.values().parallelStream().forEach((sprite -> {
            ResourceLocation location = getResourceLocation(sprite);
            if (sprite.hasCustomLoader(resourceManager, location)) {
                return;
            }

            IResource resource = null;
            try {
                PngSizeInfo pngSizeInfo = PngSizeInfo.makeFromResource(resourceManager.getResource(location));
                resource = resourceManager.getResource(location);
                boolean hasAnimation = resource.getMetadata("animation") != null;
                sprite.loadSprite(pngSizeInfo, hasAnimation);

                // Cache BufferedImage
                BufferedImage image = TextureUtil.readBufferedImage(resource.getInputStream());
                int[] rgb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), new int[image.getWidth() * image.getHeight()], 0, image.getWidth());
                SpriteBufferedImageCache.INSTANCE.put(sprite, image, rgb);

                stellar_core$cachedTextures.add(sprite);
                stellar_core$cachedLocations.add(location);
            } catch (Throwable e) {
                StellarLog.LOG.warn(e);
            } finally {
                if (resource != null) {
                    SpriteBufferedImageCache.INSTANCE.put(sprite, resource);
                }
            }
        }));
        stellar_core$cachedTextures = new ReferenceOpenHashSet<>(stellar_core$cachedTextures);
        stellar_core$cachedLocations = new ObjectOpenHashSet<>(stellar_core$cachedLocations);

        // Optifine Compat
        if (!FMLClientHandler.instance().hasOptifine()) {
            return;
        }
        if (mipmapLevels >= 4) {
            try {
                stellar_core$prevMipMapLevels = detectMaxMipmapLevelTask.get();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            mipmapLevels = 3;
        }
    }

    @Inject(method = "loadTextureAtlas", at = @At("RETURN"))
    private void injectLoadTextureTail(final IResourceManager resourceManager, final CallbackInfo ci) {
        stellar_core$cachedTextures.clear();
        stellar_core$cachedLocations.clear();
        SpriteBufferedImageCache.INSTANCE.clear();
    }

    @Redirect(method = "generateMipmaps", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/IResourceManager;getResource(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/resources/IResource;"))
    private IResource redirectGenerateMipmapsGetResource(final IResourceManager instance, final ResourceLocation resourceLocation,
                                                         @Local(name = "texture") final TextureAtlasSprite texture) throws IOException {
        IResource resource = SpriteBufferedImageCache.INSTANCE.getResourceAndRemove(texture);
        if (resource != null) {
            return resource;
        }
        return instance.getResource(resourceLocation);
    }

    @Redirect(
            method = "loadTexture(Lnet/minecraft/client/renderer/texture/Stitcher;Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;II)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/IResource;getMetadata(Ljava/lang/String;)Lnet/minecraft/client/resources/data/IMetadataSection;"
            ),
            require = 0,
            expect = 0
    )
    private IMetadataSection redirectLoadTextureGetMetadata(final IResource instance, final String s) {
        if (instance != null) {
            return instance.getMetadata(s);
        }
        return null;
    }

    @Redirect(
            method = "loadTexture(Lnet/minecraft/client/renderer/texture/Stitcher;Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;II)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;loadSprite(Lnet/minecraft/client/renderer/texture/PngSizeInfo;Z)V"
            ),
            require = 0,
            expect = 0
    )
    private void redirectLoadTextureLoadSprite(final TextureAtlasSprite instance, final PngSizeInfo sizeInfo, final boolean animations) throws Exception {
        if (!stellar_core$cachedTextures.contains(instance)) {
            instance.loadSprite(sizeInfo, animations);
        }
    }

    @Redirect(
            method = "loadTexture(Lnet/minecraft/client/renderer/texture/Stitcher;Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;II)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/PngSizeInfo;makeFromResource(Lnet/minecraft/client/resources/IResource;)Lnet/minecraft/client/renderer/texture/PngSizeInfo;"
            ),
            require = 0,
            expect = 0
    )
    private PngSizeInfo redirectLoadTextureMakeFromResource(final IResource pngsizeinfo) throws Throwable {
        if (pngsizeinfo != null) {
            return PngSizeInfo.makeFromResource(pngsizeinfo);
        }
        return null;
    }

    @Redirect(
            method = "loadTexture(Lnet/minecraft/client/renderer/texture/Stitcher;Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;II)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/IResourceManager;getResource(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/resources/IResource;"
            ),
            require = 0,
            expect = 0
    )
    private IResource redirectLoadTextureGetResource(final IResourceManager instance, final ResourceLocation resourceLocation) throws IOException {
        if (!stellar_core$cachedLocations.contains(resourceLocation)) {
            if (StellarCoreConfig.DEBUG.enableDebugLog) {
                StellarLog.LOG.info("[StellarCore-DEBUG] Loading uncached texture resource: {}", resourceLocation);
            }
            return instance.getResource(resourceLocation);
        }
        return null;
    }

    // ==================================================
    // Optifine Compat
    // ==================================================

    @Unique
    @SuppressWarnings({"deprecation", "DataFlowIssue", "RedundantCast"})
    private Future<Integer> stellar_core$initializeOptifineTask(final IResourceManager resourceManager) {
        if (!FMLClientHandler.instance().hasOptifine() || mipmapLevels < 4) {
            return null;
        }

        Method detectMaxMipmapLevel = ReflectionHelper.findMethod(TextureMap.class, "detectMaxMipmapLevel", null, Map.class, IResourceManager.class);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return (int) detectMaxMipmapLevel.invoke((TextureMap) (Object) this, mapRegisteredSprites, resourceManager);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Inject(method = "loadSprites", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureMap;initMissingImage()V"))
    private void injectLoadSpritesBeforeInitMissingImage(final IResourceManager resourceManager, final ITextureMapPopulator iconCreatorIn, final CallbackInfo ci) {
        if (!FMLClientHandler.instance().hasOptifine()) {
            return;
        }
        if (stellar_core$prevMipMapLevels >= 4) {
            mipmapLevels = stellar_core$prevMipMapLevels;
        }
    }

    @Redirect(
            method = "loadTextureAtlas",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/IResource;getMetadata(Ljava/lang/String;)Lnet/minecraft/client/resources/data/IMetadataSection;"
            ),
            require = 0,
            expect = 0
    )
    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference", "InvalidInjectorMethodSignature"})
    private IMetadataSection redirectLoadTextureAtlasGetMetadata(final IResource instance, final String s) {
        if (instance != null) {
            return instance.getMetadata(s);
        }
        return null;
    }

    @Redirect(
            method = "loadTextureAtlas",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;loadSprite(Lnet/minecraft/client/renderer/texture/PngSizeInfo;Z)V"
            ),
            require = 0,
            expect = 0
    )
    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference", "InvalidInjectorMethodSignature"})
    private void redirectLoadTextureAtlasLoadSprite(final TextureAtlasSprite instance, final PngSizeInfo sizeInfo, final boolean animations) throws Exception {
        if (!stellar_core$cachedTextures.contains(instance)) {
            instance.loadSprite(sizeInfo, animations);
        }
    }

    @Redirect(
            method = "loadTextureAtlas",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/PngSizeInfo;makeFromResource(Lnet/minecraft/client/resources/IResource;)Lnet/minecraft/client/renderer/texture/PngSizeInfo;"
            ),
            require = 0,
            expect = 0
    )
    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference", "InvalidInjectorMethodSignature"})
    private PngSizeInfo redirectLoadTextureAtlasMakeFromResource(final IResource pngsizeinfo) throws Throwable {
        if (pngsizeinfo != null) {
            return PngSizeInfo.makeFromResource(pngsizeinfo);
        }
        return null;
    }

    @Redirect(
            method = "loadTextureAtlas",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/IResourceManager;getResource(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/resources/IResource;"
            ),
            require = 0,
            expect = 0
    )
    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference", "InvalidInjectorMethodSignature"})
    private IResource redirectLoadTextureAtlasGetResource(final IResourceManager instance, final ResourceLocation resourceLocation) throws IOException {
        if (!stellar_core$cachedLocations.contains(resourceLocation)) {
            if (StellarCoreConfig.DEBUG.enableDebugLog) {
                StellarLog.LOG.info("[StellarCore-DEBUG] Loading uncached texture resource: {}", resourceLocation);
            }
            return instance.getResource(resourceLocation);
        }
        return null;
    }

}
