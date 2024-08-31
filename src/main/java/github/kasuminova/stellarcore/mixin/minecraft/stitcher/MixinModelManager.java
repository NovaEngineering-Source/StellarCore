package github.kasuminova.stellarcore.mixin.minecraft.stitcher;

import github.kasuminova.stellarcore.client.texture.StitcherCache;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.texture.TextureMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelManager.class)
public class MixinModelManager {

    @Final
    @Shadow
    private TextureMap texMap;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectFinishLoadingBefore(final TextureMap textures, final CallbackInfo ci) {
        StitcherCache.create("vanilla", texMap);
        StitcherCache.setActiveMap(texMap);
    }

}
