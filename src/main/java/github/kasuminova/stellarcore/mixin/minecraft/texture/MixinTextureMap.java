package github.kasuminova.stellarcore.mixin.minecraft.texture;

import com.google.common.collect.ImmutableMap;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.ProgressManager;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.*;

/**
 * Note: Incompatible with Optifine.
 */
@Deprecated
@SuppressWarnings({"QuestionableName", "AssignmentToMethodParameter", "StandardVariableNames", "StaticVariableMayNotBeInitialized", "NonConstantFieldWithUpperCaseName"})
@Mixin(TextureMap.class)
public abstract class MixinTextureMap {

    @Unique
    private static final ThreadLocal<Deque<ResourceLocation>> LOADING_SPRITES = ThreadLocal.withInitial(ArrayDeque::new);

    @Final
    @Shadow
    private static Logger LOGGER;

    @Shadow
    private int mipmapLevels;

    @Final
    @Mutable
    @Shadow(remap = false)
    private Set<ResourceLocation> loadedSprites;

    @Final
    @Shadow
    @Mutable
    private Map<String, TextureAtlasSprite> mapRegisteredSprites;

    @Shadow
    public abstract TextureAtlasSprite registerSprite(final ResourceLocation location);

    @Shadow
    protected abstract ResourceLocation getResourceLocation(final TextureAtlasSprite p_184396_1_);

    @Shadow
    protected abstract boolean generateMipmaps(final IResourceManager resourceManager, final TextureAtlasSprite texture);

    @Shadow(remap = false)
    protected abstract int loadTexture(final Stitcher stitcher, final IResourceManager resourceManager, final ResourceLocation location, final TextureAtlasSprite textureatlassprite, final ProgressManager.ProgressBar bar, final int j, final int k);

    @Shadow(remap = false)
    protected abstract void finishLoading(final Stitcher stitcher, final ProgressManager.ProgressBar bar, final int j, final int k);

    @Final
    @Shadow
    private Map<String, TextureAtlasSprite> mapUploadedSprites;

    @Final
    @Shadow
    private List<TextureAtlasSprite> listAnimatedSprites;

    @Inject(
            method = "<init>(Ljava/lang/String;Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;Z)V",
            at = @At("RETURN")
    )
    private void injectInit(final String basePathIn, final ITextureMapPopulator iconCreatorIn, final boolean skipFirst, final CallbackInfo ci) {
        this.mapRegisteredSprites = new NonBlockingHashMap<>();
    }

    /**
     * @author Kasumi_Nova
     * @reason Parallel loader
     */
    @Overwrite
    public void loadTextureAtlas(IResourceManager resourceManager) {
        int i = Minecraft.getGLMaximumTextureSize();
        Stitcher stitcher = new Stitcher(i, i, 0, this.mipmapLevels);
        this.mapUploadedSprites.clear();
        this.listAnimatedSprites.clear();
        int[] j = {Integer.MAX_VALUE};
        int k = 1 << this.mipmapLevels;
        net.minecraftforge.fml.common.FMLLog.log.info("Max texture size: {}", i);
        net.minecraftforge.fml.common.ProgressManager.ProgressBar bar = net.minecraftforge.fml.common.ProgressManager.push("Texture stitching", this.mapRegisteredSprites.size());
        loadedSprites.clear();

        ImmutableMap.copyOf(mapRegisteredSprites).entrySet().parallelStream().forEach(entry -> {
            final ResourceLocation location = new ResourceLocation(entry.getKey());
            synchronized (bar) {
                bar.step(location.toString());
            }
            int tmp = loadTexture(stitcher, resourceManager, location, entry.getValue(), bar, j[0], k);
            synchronized (mapRegisteredSprites) {
                if (tmp < j[0]) {
                    j[0] = tmp;
                }
            }
        });

        finishLoading(stitcher, bar, j[0], k);
    }

    /**
     * @author Kasumi_Nova
     * @reason Allow multithreaded loading
     */
    @SuppressWarnings("SynchronizeOnNonFinalField")
    @Inject(
            method = "loadTexture(Lnet/minecraft/client/renderer/texture/Stitcher;Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;II)I",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void injectLoadTexture(
            Stitcher stitcher,
            IResourceManager manager,
            ResourceLocation location,
            TextureAtlasSprite sprite,
            ProgressManager.ProgressBar bar,
            int j,
            int k,
            CallbackInfoReturnable<Integer> cir
    ) {
        cir.cancel();

        synchronized (loadedSprites) {
            if (loadedSprites.contains(location)) {
                cir.setReturnValue(j);
                return;
            }
        }

        ResourceLocation rl = this.getResourceLocation(sprite);
        IResource iresource = null;

        for (ResourceLocation loading : LOADING_SPRITES.get()) {
            if (location.equals(loading)) {
                final String error = "circular texture dependencies, stack: [" + com.google.common.base.Joiner.on(", ").join(LOADING_SPRITES.get()) + "]";
                synchronized (FMLClientHandler.instance()) {
                    FMLClientHandler.instance().trackBrokenTexture(rl, error);
                }
                cir.setReturnValue(j);
                return;
            }
        }

        LOADING_SPRITES.get().addLast(location);
        try {
            for (ResourceLocation dependency : sprite.getDependencies()) {
                if (!mapRegisteredSprites.containsKey(dependency.toString())) {
                    synchronized (mapRegisteredSprites) {
                        registerSprite(dependency);
                    }
                }
                TextureAtlasSprite depSprite = mapRegisteredSprites.get(dependency.toString());
                // Recursive
                j = loadTexture(stitcher, manager, dependency, depSprite, bar, j, k);
            }

            try {
                if (sprite.hasCustomLoader(manager, rl)) {
                    // may require synchronized
                    if (sprite.load(manager, rl, l -> mapRegisteredSprites.get(l.toString()))) {
                        cir.setReturnValue(j);
                        return;
                    }
                } else {
                    PngSizeInfo pngsizeinfo = PngSizeInfo.makeFromResource(manager.getResource(rl));
                    iresource = manager.getResource(rl);
                    boolean flag = iresource.getMetadata("animation") != null;
                    sprite.loadSprite(pngsizeinfo, flag);
                }
            } catch (RuntimeException ex) {
                synchronized (FMLClientHandler.instance()) {
                    FMLClientHandler.instance().trackBrokenTexture(rl, ex.getMessage());
                }
                cir.setReturnValue(j);
                return;
            } catch (IOException ioexception) {
                synchronized (FMLClientHandler.instance()) {
                    FMLClientHandler.instance().trackMissingTexture(rl);
                }
                cir.setReturnValue(j);
                return;
            } finally {
                IOUtils.closeQuietly(iresource);
            }

            j = Math.min(j, Math.min(sprite.getIconWidth(), sprite.getIconHeight()));
            int j1 = Math.min(Integer.lowestOneBit(sprite.getIconWidth()), Integer.lowestOneBit(sprite.getIconHeight()));

            if (j1 < k) {
                // FORGE: do not lower the mipmap level, just log the problematic textures
                LOGGER.warn("Texture {} with size {}x{} will have visual artifacts at mip level {}, it can only support level {}. Please report to the mod author that the texture should be some multiple of 16x16.", rl, sprite.getIconWidth(), sprite.getIconHeight(), MathHelper.log2(k), MathHelper.log2(j1));
            }

            if (generateMipmaps(manager, sprite)) {
                synchronized (stitcher) {
                    stitcher.addSprite(sprite);
                }
            }
            cir.setReturnValue(j);
        } finally {
            LOADING_SPRITES.get().removeLast();
            synchronized (loadedSprites) {
                loadedSprites.add(location);
            }
        }
    }

}
