package github.kasuminova.stellarcore.mixin.minecraft.forge.vanillamodeldiskcache;

import github.kasuminova.stellarcore.client.model.vanillacache.VanillaModelCaptureScope;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ModelLoaderRegistry.class, remap = false, priority = 1100)
public abstract class MixinModelLoaderRegistryCapture {
    @Redirect(method = "getModel", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/client/model/ICustomModelLoader;loadModel(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraftforge/client/model/IModel;"))
    private static IModel stellar_core$captureVanillaModel(final ICustomModelLoader loader,
                                                           final ResourceLocation location) throws Exception {
        final boolean opened = VanillaModelCaptureScope.begin(loader);
        try {
            return loader.loadModel(location);
        } finally {
            VanillaModelCaptureScope.end(opened);
        }
    }
}
