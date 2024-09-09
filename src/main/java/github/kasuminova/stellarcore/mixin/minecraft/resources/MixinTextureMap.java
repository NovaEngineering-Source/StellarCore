package github.kasuminova.stellarcore.mixin.minecraft.resources;

import net.minecraft.client.renderer.texture.TextureMap;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TextureMap.class)
public abstract class MixinTextureMap {

//    @Shadow(remap = false)
//    public abstract String getBasePath();

//    @Inject(method = "loadSprites", at = @At("HEAD"))
//    private void injectLoadSpritesBefore(final IResourceManager resourceManager, final ITextureMapPopulator iconCreatorIn, final CallbackInfo ci) {
//        if (!FMLClientHandler.instance().hasOptifine()) {
//            return;
//        }
//
//        ModelManager modelManager = ReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "modelManager", "field_175617_aL");
//        Object earlyDetectedTextures = ReflectionHelper.getPrivateValue(ModelManager.class, modelManager, "earlyDetectedTextures");
//        Collection<ResourceLocation> textures;
//        // VintageFix compat.
//        if (earlyDetectedTextures instanceof Set) {
//            StellarLog.LOG.info("[StellarCore-DEBUG] Generating resources cache!");
//            textures = (Set<ResourceLocation>) earlyDetectedTextures;
//            String emissiveSuffix = stellar_core$getEmissiveSuffix();
//            textures.parallelStream().forEach(texture -> {
//                ResourceLocation resourceLocation = stellar_core$getResourceLocation(texture);
//                Minecraft.getMinecraft().defaultResourcePack.resourceExists(resourceLocation);
//                if (emissiveSuffix == null || emissiveSuffix.isEmpty()) {
//                    return;
//                }
//                String path = resourceLocation.getPath();
//                String replacement = emissiveSuffix + ".png";
//                if (path.endsWith(replacement)) {
//                    return;
//                }
//                Minecraft.getMinecraft().defaultResourcePack.resourceExists(new ResourceLocation(
//                        resourceLocation.getNamespace(), path.replace(".png", replacement))
//                );
//            });
//        }
//    }

//    @Inject(method = "loadSprites", at = @At("RETURN"))
//    private void injectLoadSpritesAfter(final IResourceManager resourceManager, final ITextureMapPopulator iconCreatorIn, final CallbackInfo ci) {
//        ResourceExistingCache.disableCache();
//    }
//
//    @Unique
//    private ResourceLocation stellar_core$getResourceLocation(ResourceLocation loc) {
//        String path = loc.getPath().toLowerCase();
//        boolean absPath = FMLClientHandler.instance().hasOptifine() && path.startsWith("mcpatcher/") || path.startsWith("optifine/");
//        return absPath
//                ? new ResourceLocation(loc.getNamespace(), loc.getPath() + ".png")
//                : new ResourceLocation(loc.getNamespace(), String.format("%s/%s%s", getBasePath(), loc.getPath(), ".png"));
//    }
//
//    @Unique
//    private static String stellar_core$getEmissiveSuffix() {
//        if (!FMLClientHandler.instance().hasOptifine()) {
//            return null;
//        }
//
//        Class<?> EmissiveTextures;
//        try {
//            EmissiveTextures = Class.forName("net.optifine.EmissiveTextures");
//        } catch (ClassNotFoundException e) {
//            return null;
//        }
//
//        String suffixEmissive;
//        try {
//            suffixEmissive = (String) EmissiveTextures.getMethod("getSuffixEmissive").invoke(null);
//        } catch (Throwable e) {
//            return null;
//        }
//
//        return suffixEmissive == null || suffixEmissive.isEmpty() ? "e" : suffixEmissive;
//    }

}
