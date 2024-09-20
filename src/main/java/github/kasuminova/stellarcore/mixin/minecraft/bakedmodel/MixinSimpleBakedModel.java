package github.kasuminova.stellarcore.mixin.minecraft.bakedmodel;

import github.kasuminova.stellarcore.client.pool.BakedQuadPool;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Mixin(SimpleBakedModel.class)
public class MixinSimpleBakedModel {

    @Final
    @Shadow
    @Mutable
    protected List<BakedQuad> generalQuads;

    @Final
    @Shadow
    @Mutable
    protected Map<EnumFacing, List<BakedQuad>> faceQuads;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final List<BakedQuad> generalQuadsIn, final Map<EnumFacing, List<BakedQuad>> faceQuadsIn, final boolean ambientOcclusionIn, final boolean gui3dIn, final TextureAtlasSprite textureIn, final ItemCameraTransforms cameraTransformsIn, final ItemOverrideList itemOverrideListIn, final CallbackInfo ci) {
        BakedQuadPool.INSTANCE.canonicalizeAsync(() -> {
            List<BakedQuad> canonicalizedGeneralQuads = new ObjectArrayList<>();
            for (final BakedQuad quad : this.generalQuads) {
                canonicalizedGeneralQuads.add(BakedQuadPool.INSTANCE.canonicalize(quad));
            }
            this.generalQuads = canonicalizedGeneralQuads;

            Map<EnumFacing, List<BakedQuad>> canonicalizedFaceQuads = new EnumMap<>(EnumFacing.class);
            this.faceQuads.forEach((facing, quads) -> {
                List<BakedQuad> canonicalizedQuads = new ObjectArrayList<>();
                for (final BakedQuad quad : quads) {
                    canonicalizedQuads.add(BakedQuadPool.INSTANCE.canonicalize(quad));
                }
                canonicalizedFaceQuads.put(facing, canonicalizedQuads);
            });
            this.faceQuads = canonicalizedFaceQuads;
        });
    }

}
