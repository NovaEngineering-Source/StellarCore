package github.kasuminova.stellarcore.client.integration.enderutilities;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.IModelState;

import java.util.function.Function;

/**
 * Unbaked models and the texture getter stay local to the bake call.
 */
public final class NullifierModelFactory {
    private static final ResourceLocation BASE = new ResourceLocation("enderutilities", "item/nullifier_base");
    private static final ResourceLocation LOCKED = new ResourceLocation("enderutilities", "item/standard_item");

    private NullifierModelFactory() {
    }

    public static IBakedModel bake(final IModelState state, final VertexFormat format,
                                   final Function<ResourceLocation, TextureAtlasSprite> textures) {
        final IModel base = ModelLoaderRegistry.getModelOrMissing(BASE);
        final IModel locked = ModelLoaderRegistry.getModelOrMissing(LOCKED)
                                                 .retexture(ImmutableMap.of("layer0", "enderutilities:items/item_overlay_locked"));
        return bake(base, locked, state, format, textures);
    }

    static IBakedModel bake(final IModel base, final IModel locked, final IModelState state, final VertexFormat format,
                            final Function<ResourceLocation, TextureAtlasSprite> textures) {
        return new NullifierBakedModel(base.bake(state, format, textures), locked.bake(state, format, textures),
            PerspectiveMapWrapper.getTransforms(state), NullifierQuadTransform.placement(state));
    }
}
