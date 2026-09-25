package github.kasuminova.stellarcore.mixin.minecraft.forge.vanillamodeldiskcache;

import github.kasuminova.stellarcore.client.model.vanillacache.VanillaModelDiskCache;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.animation.ModelBlockAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$VanillaLoader", remap = false)
public abstract class MixinVanillaLoaderArmatureCache {

    @Redirect(
            method = "loadModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/client/model/animation/ModelBlockAnimation;loadVanillaAnimation(Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;)Lnet/minecraftforge/client/model/animation/ModelBlockAnimation;",
                    remap = false
            ),
            remap = false
    )
    private ModelBlockAnimation stellar_core$serveAnimationFromDiskCache(
            final IResourceManager manager,
            final ResourceLocation armatureLocation) {
        final VanillaModelDiskCache cache = VanillaModelDiskCache.INSTANCE;
        if (cache.isEnabled()) {
            final ResourceLocation modelLocation = stellar_core$modelLocationFromArmature(armatureLocation);
            if (modelLocation != null) {
                try {
                    final ModelBlockAnimation cached = cache.tryBuildAnimation(modelLocation);
                    if (cached != null) {
                        return cached;
                    }
                } catch (Throwable ignored) {
                    // The disk cache must never break the load pipeline.
                }
            }
        }
        return ModelBlockAnimation.loadVanillaAnimation(manager, armatureLocation);
    }

    @Unique
    private static ResourceLocation stellar_core$modelLocationFromArmature(final ResourceLocation armatureLocation) {
        final String path = armatureLocation.getPath();
        if (!path.startsWith("armatures/") || !path.endsWith(".json")) {
            return null;
        }
        final String modelPath = path.substring("armatures/".length(), path.length() - ".json".length());
        return new ResourceLocation(armatureLocation.getNamespace(), modelPath);
    }
}
