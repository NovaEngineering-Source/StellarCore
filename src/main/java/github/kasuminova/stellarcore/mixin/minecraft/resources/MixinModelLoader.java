package github.kasuminova.stellarcore.mixin.minecraft.resources;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.client.resource.ClasspathAssetIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ModelLoader.class)
public class MixinModelLoader extends ModelBakery {

    @SuppressWarnings("DataFlowIssue")
    public MixinModelLoader() {
        super(null, null, null);
    }

    @Inject(
            method = "setupModelRegistry",
            at = @At("HEAD")
    )
    private void stellar_core$ensureClasspathIndexReady(final CallbackInfoReturnable<IRegistry<ModelResourceLocation, IBakedModel>> cir) {
        if (!FMLClientHandler.instance().hasOptifine()) {
            return;
        }

        // OptiFine may query emissive resources during registerSprite/checkEmissive,
        // which happens before the later loadSprites INVOKE injection.
        ClasspathAssetIndex.prewarm(java.util.Collections.singleton("minecraft"));
    }

    @Inject(
            method = "setupModelRegistry",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureMap;loadSprites(Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;)V"
            )
    )
    private void injectSetupModelRegistryBeforeLoadSprites(final CallbackInfoReturnable<IRegistry<ModelResourceLocation, IBakedModel>> cir,
                                                           @Local(name = "textures") final Set<ResourceLocation> textures) {
        String emissiveSuffix = stellar_core$getEmissiveSuffix();
        final Set<String> defaultResourceDomains = Minecraft.getMinecraft().defaultResourcePack.getResourceDomains();

        textures.parallelStream().forEach(texture -> {
            if (!defaultResourceDomains.contains(texture.getNamespace())) {
                return;
            }
            ResourceLocation resourceLocation = stellar_core$getResourceLocation(texture);
            Minecraft.getMinecraft().defaultResourcePack.resourceExists(resourceLocation);
            if (emissiveSuffix == null || emissiveSuffix.isEmpty()) {
                return;
            }
            String path = resourceLocation.getPath();
            String replacement = emissiveSuffix + ".png";
            if (path.endsWith(replacement)) {
                return;
            }
            Minecraft.getMinecraft().defaultResourcePack.resourceExists(new ResourceLocation(
                    resourceLocation.getNamespace(), path.replace(".png", replacement))
            );
        });
    }

    @Unique
    private ResourceLocation stellar_core$getResourceLocation(ResourceLocation loc) {
        String path = loc.getPath().toLowerCase();
        boolean absPath = FMLClientHandler.instance().hasOptifine() && path.startsWith("mcpatcher/") || path.startsWith("optifine/");
        return absPath 
                ? new ResourceLocation(loc.getNamespace(), loc.getPath() + ".png") 
                : new ResourceLocation(loc.getNamespace(), String.format("%s/%s%s", this.textureMap.getBasePath(), loc.getPath(), ".png"));
    }

    @Unique
    private static String stellar_core$getEmissiveSuffix() {
        if (!FMLClientHandler.instance().hasOptifine()) {
            return null;
        }

        Class<?> EmissiveTextures;
        try {
            EmissiveTextures = Class.forName("net.optifine.EmissiveTextures");
        } catch (ClassNotFoundException e) {
            return null;
        }

        String suffixEmissive;
        try {
            suffixEmissive = (String) EmissiveTextures.getMethod("getSuffixEmissive").invoke(null);
        } catch (Throwable e) {
            return null;
        }

        return suffixEmissive;
    }

}
