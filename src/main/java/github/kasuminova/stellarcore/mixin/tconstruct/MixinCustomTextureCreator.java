package github.kasuminova.stellarcore.mixin.tconstruct;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.library.client.CustomTextureCreator;
import slimeknights.tconstruct.library.tools.IToolPart;

import java.util.Map;
import java.util.Set;

@Mixin(value = CustomTextureCreator.class, remap = false)
public class MixinCustomTextureCreator {

    @Shadow
    public static Map<String, Map<String, TextureAtlasSprite>> sprites;

    @Shadow
    private static Set<ResourceLocation> baseTextures;

    @Shadow
    private static Map<ResourceLocation, Set<IToolPart>> texturePartMapping;

    @Inject(method = "<clinit>", at = @At("RETURN"), remap = false)
    private static void injectCLInit(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader) {
            return;
        }
        sprites = new NonBlockingHashMap<>();
        baseTextures = new NonBlockingHashSet<>();
        texturePartMapping = new NonBlockingHashMap<>();
    }

}
