package github.kasuminova.stellarcore.mixin.minecraft.texturemap;

import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.texture.ITextureMapPopulator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
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

    @Final
    @Shadow(remap = false)
    @Mutable
    private Set<ResourceLocation> loadedSprites;

    @Inject(method = "<init>(Ljava/lang/String;Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;Z)V", at = @At("RETURN"))
    private void injectInit(final String basePathIn, final ITextureMapPopulator iconCreatorIn, final boolean skipFirst, final CallbackInfo ci) {
        this.mapUploadedSprites = new Object2ObjectOpenHashMap<>();
        this.loadedSprites = new ObjectOpenHashSet<>();

        // When model loading is parallelized, some mods may register sprites from multiple
        // threads. Vanilla uses HashMap which is not thread-safe and can randomly drop entries.
        // Use a concurrent map to keep sprite registration deterministic.
        this.mapRegisteredSprites = new NonBlockingHashMap<>();
    }

    @Inject(method = "loadTextureAtlas", at = @At("RETURN"), require = 1)
    private void stellar_core$releaseLoadedSpriteLocations(final IResourceManager resourceManager, final CallbackInfo ci) {
        // Only the recursive loading pass reads this set. A later atlas reload
        // starts a fresh pass; uploaded sprites are kept in mapUploadedSprites.
        loadedSprites = new ObjectOpenHashSet<>(0);
    }

}
