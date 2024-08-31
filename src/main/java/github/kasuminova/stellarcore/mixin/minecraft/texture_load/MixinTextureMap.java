package github.kasuminova.stellarcore.mixin.minecraft.texture_load;

import github.kasuminova.stellarcore.StellarCore;
import github.kasuminova.stellarcore.client.texture.SpriteBufferedImageCache;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(TextureMap.class)
public abstract class MixinTextureMap {

    @Final
    @Shadow
    private Map<String, TextureAtlasSprite> mapRegisteredSprites;

    @Shadow
    protected abstract ResourceLocation getResourceLocation(final TextureAtlasSprite p_184396_1_);

    @Unique
    private Set<TextureAtlasSprite> stellar_core$cachedTextures;

    @Unique
    private Set<ResourceLocation> stellar_core$cachedLocations;

    @Inject(method = "loadTextureAtlas", at = @At("HEAD"))
    private void injectLoadTextureAtlas(final IResourceManager resourceManager, final CallbackInfo ci) {
        SpriteBufferedImageCache.INSTANCE.clear();
        stellar_core$cachedTextures = Collections.newSetFromMap(new ConcurrentHashMap<>());
        stellar_core$cachedLocations = Collections.newSetFromMap(new ConcurrentHashMap<>());
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
                SpriteBufferedImageCache.INSTANCE.put(sprite, TextureUtil.readBufferedImage(resource.getInputStream()));

                synchronized (stellar_core$cachedTextures) {
                    stellar_core$cachedTextures.add(sprite);
                    stellar_core$cachedLocations.add(location);
                }
            } catch (Throwable e) {
                StellarCore.log.warn(e);
            } finally {
                IOUtils.closeQuietly(resource);
            }
        }));
    }

    @Inject(method = "loadTextureAtlas", at = @At("RETURN"))
    private void injectLoadTextureTail(final IResourceManager resourceManager, final CallbackInfo ci) {
        stellar_core$cachedTextures.clear();
        stellar_core$cachedLocations.clear();
        SpriteBufferedImageCache.INSTANCE.clear();
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
            return instance.getResource(resourceLocation);
        }
        return null;
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
    private IResource redirectLoadTextureAtlasGetResource(final IResourceManager instance, final ResourceLocation resourceLocation) throws IOException {
        if (!stellar_core$cachedLocations.contains(resourceLocation)) {
            return instance.getResource(resourceLocation);
        }
        return null;
    }

}
