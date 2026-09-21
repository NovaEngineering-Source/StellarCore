package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import github.kasuminova.stellarcore.client.model.ModelDefinitionFlight;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

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
    private final Map<ResourceLocation, ModelDefinitionFlight> stellar_core$definitionFlights = new ConcurrentHashMap<>();

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

        final ModelDefinitionFlight existing = this.stellar_core$definitionFlights.get(resourcelocation);
        if (existing != null) {
            if (existing.owner == Thread.currentThread()) {
                return this.stellar_core$loadDefinition(location, resourcelocation);
            }
            try {
                return existing.future.get();
            } catch (final InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for blockstate " + resourcelocation, interrupted);
            } catch (final ExecutionException failure) {
                final Throwable cause = failure.getCause();
                if (cause instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException("Failed while waiting for blockstate " + resourcelocation, cause);
            }
        }

        final ModelDefinitionFlight created = new ModelDefinitionFlight(Thread.currentThread());
        final ModelDefinitionFlight previous = this.stellar_core$definitionFlights.putIfAbsent(resourcelocation, created);
        if (previous != null) {
            if (previous.owner == Thread.currentThread()) {
                return this.stellar_core$loadDefinition(location, resourcelocation);
            }
            try {
                return previous.future.get();
            } catch (final InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for blockstate " + resourcelocation, interrupted);
            } catch (final ExecutionException failure) {
                final Throwable cause = failure.getCause();
                if (cause instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException("Failed while waiting for blockstate " + resourcelocation, cause);
            }
        }

        try {
            final ModelBlockDefinition loaded = this.stellar_core$loadDefinition(location, resourcelocation);
            created.future.complete(loaded);
            return loaded;
        } catch (final Throwable failure) {
            created.future.completeExceptionally(failure);
            throw failure;
        } finally {
            this.stellar_core$definitionFlights.remove(resourcelocation, created);
        }
    }

    @Unique
    private ModelBlockDefinition stellar_core$loadDefinition(final ResourceLocation location, final ResourceLocation resourceLocation) {
        final int[] counter = stellar_core$LOADED_RESOURCE_COUNT.get();
        counter[0] = 0;
        final ModelBlockDefinition loaded = this.loadMultipartMBD(location, resourceLocation);
        final int loadedCount = counter[0];
        final Integer bestKnown = stellar_core$definitionResourceCounts.get(resourceLocation);
        if (bestKnown != null && loadedCount < bestKnown) {
            return loaded;
        }
        stellar_core$definitionResourceCounts.put(resourceLocation, loadedCount);
        this.blockDefinitions.put(resourceLocation, loaded);
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
