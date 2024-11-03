package github.kasuminova.stellarcore.client.integration.railcraft;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Function;

public class RCModelBaker {

    @Nullable
    public static IBakedModel load(final Set<ModelResourceLocation> modelRL, final IModel model, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
        boolean detected = false;
        for (final ModelResourceLocation rl : modelRL) {
            if (rl.getNamespace().startsWith("railcraft")) {
                detected = true;
                break;
            }
        }
        if (detected) {
            synchronized (RCModelBaker.class) {
                return model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM, textureGetter);
            }
        }
        return null;
    }

}
