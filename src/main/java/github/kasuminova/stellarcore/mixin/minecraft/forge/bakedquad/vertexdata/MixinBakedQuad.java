package github.kasuminova.stellarcore.mixin.minecraft.forge.bakedquad.vertexdata;

import github.kasuminova.stellarcore.mixin.util.AccessorBakedQuad;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BakedQuad.class)
public class MixinBakedQuad implements AccessorBakedQuad {

    @Final
    @Shadow
    @Mutable
    protected int[] vertexData;

    @Override
    public void stellar_core$setVertexData(final int[] vertexData) {
        this.vertexData = vertexData;
    }

}
