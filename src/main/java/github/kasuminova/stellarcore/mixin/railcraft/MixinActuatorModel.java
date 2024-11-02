package github.kasuminova.stellarcore.mixin.railcraft;

import mods.railcraft.client.render.models.resource.ActuatorModel;
import net.minecraft.block.state.IBlockState;
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
import java.util.function.Function;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = ActuatorModel.class, remap = false)
public abstract class MixinActuatorModel {

    @Shadow
    private static void bakeModels(final Map<ModelResourceLocation, IBakedModel> models,
                                   final VertexFormat format,
                                   final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
                                   final Map<IBlockState, ModelResourceLocation> modelLocations) {
    }

    @Redirect(
            method = "bake",
            at = @At(
                    value = "INVOKE",
                    target = "Lmods/railcraft/client/render/models/resource/ActuatorModel;bakeModels(Ljava/util/Map;Lnet/minecraft/client/renderer/vertex/VertexFormat;Ljava/util/function/Function;Ljava/util/Map;)V"
            )
    )
    private void injectBakeModels(final Map<ModelResourceLocation, IBakedModel> model, final VertexFormat modelLocation, final Function<ResourceLocation, TextureAtlasSprite> models, final Map<IBlockState, ModelResourceLocation> format) {
        synchronized (ActuatorModel.class) {
            bakeModels(model, modelLocation, models, format);
        }
    }

}
