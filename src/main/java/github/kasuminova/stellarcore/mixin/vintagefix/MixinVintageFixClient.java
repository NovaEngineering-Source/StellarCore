package github.kasuminova.stellarcore.mixin.vintagefix;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.embeddedt.vintagefix.VintageFixClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = VintageFixClient.class, remap = false)
public class MixinVintageFixClient {

    @Inject(method = "collectTextures", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"))
    private void injectCollectTextures(final TextureStitchEvent.Pre event, final CallbackInfo ci,
                                       @Local(name = "allTextures") Set<ResourceLocation> allTextures,
                                       @Local(name = "map") TextureMap map)
    {
        if (!FMLClientHandler.instance().hasOptifine()) {
            return;
        }

        String emissiveSuffix = stellar_core$getEmissiveSuffix();
        final Set<String> defaultResourceDomains = Minecraft.getMinecraft().defaultResourcePack.getResourceDomains();
        allTextures.parallelStream().forEach(texture -> {
            if (!defaultResourceDomains.contains(texture.getNamespace())) {
                return;
            }
            ResourceLocation resourceLocation = stellar_core$getResourceLocation(texture, map);
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
    private static ResourceLocation stellar_core$getResourceLocation(ResourceLocation loc, TextureMap map) {
        String path = loc.getPath().toLowerCase();
        boolean absPath = path.startsWith("mcpatcher/") || path.startsWith("optifine/");
        return absPath
                ? new ResourceLocation(loc.getNamespace(), loc.getPath() + ".png")
                : new ResourceLocation(loc.getNamespace(), String.format("%s/%s%s", map.getBasePath(), loc.getPath(), ".png"));
    }

    @Unique
    private static String stellar_core$getEmissiveSuffix() {
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
