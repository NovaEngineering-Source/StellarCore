package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.ITextureMapPopulator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(TextureMap.class)
public class MixinTextureMap {

    @Final
    @Shadow
    @Mutable
    private Map<String, TextureAtlasSprite> mapRegisteredSprites;

    @Inject(method = "<init>(Ljava/lang/String;Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;Z)V", at = @At("RETURN"))
    private void injectInit(final String basePathIn, final ITextureMapPopulator iconCreatorIn, final boolean skipFirst, final CallbackInfo ci) {
        if (StellarCoreConfig.PERFORMANCE.vanilla.parallelTextureMapLoad || StellarCoreConfig.PERFORMANCE.vanilla.stitcherCache) {
            return;
        }
        this.mapRegisteredSprites = new ConcurrentHashMap<>();
    }

}
