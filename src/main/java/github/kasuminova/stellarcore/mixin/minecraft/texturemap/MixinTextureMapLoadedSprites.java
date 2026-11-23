package github.kasuminova.stellarcore.mixin.minecraft.texturemap;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.texture.ITextureMapPopulator;
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

import java.util.Set;

@Mixin(TextureMap.class)
public class MixinTextureMapLoadedSprites {

    // Forge-only field; OptiFine tracks dependencies separately.
    @Final
    @Shadow(remap = false)
    @Mutable
    private Set<ResourceLocation> loadedSprites;

    @Inject(method = "<init>(Ljava/lang/String;Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;Z)V", at = @At("RETURN"), require = 1)
    private void stellar_core$initializeLoadedSpriteLocations(final String basePathIn, final ITextureMapPopulator iconCreatorIn,
                                                              final boolean skipFirst, final CallbackInfo ci) {
        this.loadedSprites = new ObjectOpenHashSet<>();
    }

    @Inject(method = "loadTextureAtlas", at = @At("RETURN"), require = 1)
    private void stellar_core$releaseLoadedSpriteLocations(final IResourceManager resourceManager, final CallbackInfo ci) {
        // The recursive loading pass is done. Drop its backing table, not just its entries.
        this.loadedSprites = new ObjectOpenHashSet<>(0);
    }

}
