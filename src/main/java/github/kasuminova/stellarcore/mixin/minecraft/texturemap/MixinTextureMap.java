package github.kasuminova.stellarcore.mixin.minecraft.texturemap;

import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
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

    @Inject(method = "<init>(Ljava/lang/String;Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;Z)V", at = @At("RETURN"), require = 1)
    private void injectInit(final String basePathIn, final ITextureMapPopulator iconCreatorIn, final boolean skipFirst, final CallbackInfo ci) {
        this.mapUploadedSprites = new Object2ObjectOpenHashMap<>();
        // Mods may register sprites off-thread during parallel loading. The atlas also
        // snapshots this map, so it must support traversal while registrations occur.
        this.mapRegisteredSprites = new NonBlockingHashMap<>();
    }

}
