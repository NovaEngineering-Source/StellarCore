package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import github.kasuminova.stellarcore.mixin.util.StellarCoreModelBakery;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.VariantList;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"MethodMayBeStatic", "FieldAccessedSynchronizedAndUnsynchronized"})
@Mixin(ModelBakery.class)
public abstract class MixinModelBakery implements StellarCoreModelBakery {

    @Shadow
    protected abstract ResourceLocation getBlockstateLocation(final ResourceLocation location);

    @Final
    @Shadow
    @Mutable
    private Map<ResourceLocation, ModelBlockDefinition> blockDefinitions;

    @Shadow
    protected abstract ModelBlockDefinition loadMultipartMBD(final ResourceLocation location, final ResourceLocation fileIn);

    @Final
    @Shadow
    @Mutable
    private Map<ModelResourceLocation, VariantList> variants;

    @Final
    @Shadow
    @Mutable
    private Map<ModelBlockDefinition, Collection<ModelResourceLocation>> multipartVariantMap;

    @Unique
    private Map<ResourceLocation, Integer> stellar_core$definitionResourceCounts;

    @Unique
    private static final ThreadLocal<int[]> stellar_core$LOADED_RESOURCE_COUNT = ThreadLocal.withInitial(() -> new int[1]);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final IResourceManager resourceManagerIn, final TextureMap textureMapIn, final BlockModelShapes blockModelShapesIn, final CallbackInfo ci) {
        blockDefinitions = new NonBlockingHashMap<>();
        variants = new NonBlockingHashMap<>();
        multipartVariantMap = new NonBlockingHashMap<>();
        stellar_core$definitionResourceCounts = new NonBlockingHashMap<>();
    }

    @Redirect(
        method = "loadMultipartMBD",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/IResourceManager;getAllResources(Lnet/minecraft/util/ResourceLocation;)Ljava/util/List;"
        )
    )
    private List<IResource> stellar_core$countLoadedResources(final IResourceManager instance, final ResourceLocation location) throws IOException {
        final List<IResource> resources = instance.getAllResources(location);
        stellar_core$LOADED_RESOURCE_COUNT.get()[0] = resources == null ? 0 : resources.size();
        return resources;
    }

    /**
     * @author Kasumi_Nova
     * @reason Thread Safe.
     */
    @Overwrite
    protected ModelBlockDefinition getModelBlockDefinition(ResourceLocation location) {
        ResourceLocation resourcelocation = this.getBlockstateLocation(location);

        ModelBlockDefinition cached = this.blockDefinitions.get(resourcelocation);
        if (cached != null) {
            return cached;
        }

        final int[] counter = stellar_core$LOADED_RESOURCE_COUNT.get();
        counter[0] = 0;
        ModelBlockDefinition loaded = this.loadMultipartMBD(location, resourcelocation);
        final int loadedCount = counter[0];

        final Integer bestKnown = stellar_core$definitionResourceCounts.get(resourcelocation);
        if (bestKnown != null && loadedCount < bestKnown) {
            return loaded;
        }
        stellar_core$definitionResourceCounts.put(resourcelocation, loadedCount);

        this.blockDefinitions.put(resourcelocation, loaded);
        return loaded;
    }

    @Unique
    @Override
    public void stellar_core$invalidateBlockDefinition(final ResourceLocation location) {
        if (location == null) {
            return;
        }
        this.blockDefinitions.remove(this.getBlockstateLocation(location));
    }

}
