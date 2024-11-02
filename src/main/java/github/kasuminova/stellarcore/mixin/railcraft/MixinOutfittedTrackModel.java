package github.kasuminova.stellarcore.mixin.railcraft;

import mods.railcraft.client.render.models.resource.ActuatorModel;
import mods.railcraft.client.render.models.resource.OutfittedTrackModel;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Mixin(value = OutfittedTrackModel.class, remap = false)
public abstract class MixinOutfittedTrackModel {

    @Shadow
    protected abstract Map<ModelResourceLocation, IBakedModel> bakeModels(
            final VertexFormat format,
            final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
            final Set<ModelResourceLocation> modelLocations
    );

    @Redirect(
            method = "bake",
            at = @At(
                    value = "INVOKE",
                    target = "Lmods/railcraft/client/render/models/resource/OutfittedTrackModel;bakeModels(Lnet/minecraft/client/renderer/vertex/VertexFormat;Ljava/util/function/Function;Ljava/util/Set;)Ljava/util/Map;"
            )
    )
    private Map<ModelResourceLocation, IBakedModel> injectBakeModels(final OutfittedTrackModel instance, final VertexFormat format, final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, final Set<ModelResourceLocation> modelLocations) {
        synchronized (ActuatorModel.class) {
            return bakeModels(format, bakedTextureGetter, modelLocations);
        }
    }

}
