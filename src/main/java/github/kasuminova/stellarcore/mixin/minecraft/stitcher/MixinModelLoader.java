package github.kasuminova.stellarcore.mixin.minecraft.stitcher;

import github.kasuminova.stellarcore.client.texture.StitcherCache;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.model.ModelLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(ModelLoader.class)
public class MixinModelLoader extends ModelBakery {

    @SuppressWarnings("DataFlowIssue")
    public MixinModelLoader() {
        super(null, null, null);
    }

    @Inject(method = "setupModelRegistry",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureMap;loadSprites(Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;)V"
            )
    )
    private void injectFinishLoadingBefore(final CallbackInfoReturnable<IRegistry<ModelResourceLocation, IBakedModel>> cir) {
        StitcherCache.create("vanilla", textureMap);
        StitcherCache.setActiveMap(textureMap);
    }

    @Inject(method = "setupModelRegistry",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureMap;loadSprites(Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void injectFinishLoadingAfter(final CallbackInfoReturnable<IRegistry<ModelResourceLocation, IBakedModel>> cir) {
        StitcherCache.setActiveMap(null);
    }

}
