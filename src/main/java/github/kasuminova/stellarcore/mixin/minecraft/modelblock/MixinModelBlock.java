package github.kasuminova.stellarcore.mixin.minecraft.modelblock;

import github.kasuminova.stellarcore.client.util.AutoCanonizingStringMap;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ModelBlock.class)
public class MixinModelBlock {

    @Final
    @Shadow
    @Mutable
    public Map<String, String> textures;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final ResourceLocation parentLocationIn, final List elementsIn, final Map texturesIn, final boolean ambientOcclusionIn, final boolean gui3dIn, final ItemCameraTransforms cameraTransformsIn, final List overridesIn, final CallbackInfo ci) {
        textures = new AutoCanonizingStringMap<>(textures);
    }

}
