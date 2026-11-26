package github.kasuminova.stellarcore.mixin.enderutilities;

import github.kasuminova.stellarcore.client.integration.enderutilities.NullifierModelFactory;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.IModelState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

import java.util.function.Function;

@Pseudo
@Mixin(targets = "fi.dy.masa.enderutilities.client.renderer.model.ModelNullifierBaked$ModelNullifier", remap = false)
public abstract class MixinNullifierModelBake {
    /** @author StellarCore @reason Build the runtime compositor without retaining legacy unbaked state. */
    @Overwrite
    public IBakedModel bake(final IModelState state, final VertexFormat format,
                            final Function<ResourceLocation, TextureAtlasSprite> textures) {
        return NullifierModelFactory.bake(state, format, textures);
    }
}
