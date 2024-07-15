package github.kasuminova.stellarcore.mixin.minecraft.forge;

import com.google.common.base.Joiner;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.spongepowered.asm.mixin.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"StaticVariableUsedBeforeInitialization", "StaticVariableMayNotBeInitialized"})
@Mixin(value = ModelLoaderRegistry.class, remap = false)
public abstract class MixinModelLoaderRegistry {

    @Unique
    private static final ThreadLocal<Deque<ResourceLocation>> LOADING_MODELS = ThreadLocal.withInitial(ArrayDeque::new);

    @Final
    @Shadow
    private static Map<ResourceLocation, IModel> cache;

    @Final
    @Shadow
    private static Map<ResourceLocation, ResourceLocation> aliases;

    @Final
    @Shadow
    private static Set<ICustomModelLoader> loaders;

    @Final
    @Shadow
    private static Set<ResourceLocation> textures;

    @Shadow
    public static ResourceLocation getActualLocation(final ResourceLocation location) {
        return null;
    }

    @Shadow
    public static IModel getMissingModel() {
        return null;
    }

    @Shadow
    public static IModel getModelOrMissing(final ResourceLocation location) {
        return null;
    }

    /**
     * @author Kasumi_Nova
     * @reason Allow multithreaded model loading.
     */
    @Overwrite
    public static IModel getModel(ResourceLocation location) throws Exception {
        IModel model;

        IModel cached = cache.get(location);
        if (cached != null) return cached;

        for (ResourceLocation loading : LOADING_MODELS.get()) {
            if (location.getClass() == loading.getClass() && location.equals(loading)) {
                throw new ModelLoaderRegistry.LoaderException("circular model dependencies, stack: [" + Joiner.on(", ").join(LOADING_MODELS.get()) + "]");
            }
        }
        LOADING_MODELS.get().addLast(location);
        try {
            ResourceLocation aliased = aliases.get(location);
            if (aliased != null) return getModel(aliased);

            ResourceLocation actual = getActualLocation(location);
            ICustomModelLoader accepted = null;
            for (ICustomModelLoader loader : loaders) {
                try {
                    if (loader.accepts(actual)) {
                        if (accepted != null) {
                            throw new ModelLoaderRegistry.LoaderException(String.format("2 loaders (%s and %s) want to load the same model %s", accepted, loader, location));
                        }
                        accepted = loader;
                    }
                } catch (Exception e) {
                    throw new ModelLoaderRegistry.LoaderException(String.format("Exception checking if model %s can be loaded with loader %s, skipping", location, loader), e);
                }
            }

            // no custom loaders found, try vanilla ones
            if (accepted == null) {
                ICustomModelLoader variantLoader = stellar_core$getVariantLoader();
                if (variantLoader.accepts(actual)) {
                    accepted = variantLoader;
                } else {
                    ICustomModelLoader vanillaLoader = stellar_core$getVanillaLoader();
                    if (vanillaLoader.accepts(actual)) {
                        accepted = vanillaLoader;
                    }
                }
            }

            if (accepted == null) {
                throw new ModelLoaderRegistry.LoaderException("no suitable loader found for the model " + location + ", skipping");
            }
            try {
                model = accepted.loadModel(actual);
            } catch (Exception e) {
                throw new ModelLoaderRegistry.LoaderException(String.format("Exception loading model %s with loader %s, skipping", location, accepted), e);
            }
            if (model == getMissingModel()) {
                throw new ModelLoaderRegistry.LoaderException(String.format("Loader %s returned missing model while loading model %s", accepted, location));
            }
            if (model == null) {
                throw new ModelLoaderRegistry.LoaderException(String.format("Loader %s returned null while loading model %s", accepted, location));
            }
            synchronized (textures) {
                textures.addAll(model.getTextures());
            }
        } finally {
            ResourceLocation popLoc = LOADING_MODELS.get().removeLast();
            if (popLoc != location) {
                throw new IllegalStateException("Corrupted loading model stack: " + popLoc + " != " + location);
            }
        }
        synchronized (cache) {
            cache.put(location, model);
        }
        for (ResourceLocation dep : model.getDependencies()) {
            getModelOrMissing(dep);
        }
        return model;
    }

    @Unique
    private static ICustomModelLoader stellar_core$getVariantLoader() {
        try {
            return (ICustomModelLoader) Class.forName("net.minecraftforge.client.model.ModelLoader$VariantLoader").getEnumConstants()[0];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private static ICustomModelLoader stellar_core$getVanillaLoader() {
        try {
            return (ICustomModelLoader) Class.forName("net.minecraftforge.client.model.ModelLoader$VanillaLoader").getEnumConstants()[0];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
