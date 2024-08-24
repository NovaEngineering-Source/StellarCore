package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import com.google.common.base.Joiner;
import github.kasuminova.stellarcore.client.model.ParallelModelLoaderAsyncBlackList;
import github.kasuminova.stellarcore.mixin.StellarCoreEarlyMixinLoader;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("StaticVariableMayNotBeInitialized")
@Mixin(value = ModelLoaderRegistry.class, remap = false)
public abstract class MixinModelLoaderRegistry {

    @Unique
    private static final ThreadLocal<Deque<ResourceLocation>> stellar_core$LOADING_MODELS = ThreadLocal.withInitial(ArrayDeque::new);

    @Unique
    private static final Map<ResourceLocation, IModel> stellar_core$CACHE = new ConcurrentHashMap<>();

    @Unique
    private static final Map<ResourceLocation, ResourceLocation> stellar_core$ALIASES = new ConcurrentHashMap<>();

    @Unique
    private static final Set<ResourceLocation> stellar_core$TEXTURES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Final
    @Shadow
    private static Set<ICustomModelLoader> loaders;

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

    @Shadow
    private static IResourceManager manager;

    @Inject(method = "registerLoader", at = @At("RETURN"), remap = false)
    private static void injectRegisterLoader(final ICustomModelLoader loader, final CallbackInfo ci) {
        Class<? extends ICustomModelLoader> loaderClass = loader.getClass();
        StellarCoreEarlyMixinLoader.LOG.info("[StellarCore-MixinModelLoaderRegistry] Registered model loader: {}, AsyncBlackListed: {}",
                loaderClass.getName(),
                ParallelModelLoaderAsyncBlackList.INSTANCE.isInBlackList(loaderClass)
        );
    }

    /**
     * @author Kasumi_Nova
     * @reason Allow multithreaded model loading.
     */
    @Overwrite
    public static IModel getModel(ResourceLocation location) throws Exception {
        IModel model;

        IModel cached = stellar_core$CACHE.get(location);
        if (cached != null) {
            return cached;
        }

        for (ResourceLocation loading : stellar_core$LOADING_MODELS.get()) {
            if (location.getClass() == loading.getClass() && location.equals(loading)) {
                throw new ModelLoaderRegistry.LoaderException("circular model dependencies, stack: [" + Joiner.on(", ").join(stellar_core$LOADING_MODELS.get()) + "]");
            }
        }
        stellar_core$LOADING_MODELS.get().addLast(location);
        try {
            synchronized (stellar_core$ALIASES) {
                ResourceLocation aliased = stellar_core$ALIASES.get(location);
                if (aliased != null) {
                    return getModel(aliased);
                }
            }

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
                if (ParallelModelLoaderAsyncBlackList.INSTANCE.isInBlackList(accepted.getClass())) {
                    synchronized (accepted) {
                        model = accepted.loadModel(actual);
                    }
                } else {
                    model = accepted.loadModel(actual);
                }
            } catch (Exception e) {
                throw new ModelLoaderRegistry.LoaderException(String.format("Exception loading model %s with loader %s, skipping", location, accepted), e);
            }
            if (model == getMissingModel()) {
                throw new ModelLoaderRegistry.LoaderException(String.format("Loader %s returned missing model while loading model %s", accepted, location));
            }
            if (model == null) {
                throw new ModelLoaderRegistry.LoaderException(String.format("Loader %s returned null while loading model %s", accepted, location));
            }
            stellar_core$TEXTURES.addAll(model.getTextures());
        } finally {
            ResourceLocation popLoc = stellar_core$LOADING_MODELS.get().removeLast();
            if (popLoc != location) {
                throw new IllegalStateException("Corrupted loading model stack: " + popLoc + " != " + location);
            }
        }
        stellar_core$CACHE.put(location, model);
        for (ResourceLocation dep : model.getDependencies()) {
            getModelOrMissing(dep);
        }
        return model;
    }

    /**
     * @author Kasumi_Nova
     * @reason Allow multithreaded model loading.
     */
    @Overwrite
    public static boolean loaded(ResourceLocation location) {
        return stellar_core$CACHE.containsKey(location);
    }

    /**
     * @author Kasumi_Nova
     * @reason Allow multithreaded model loading.
     */
    @Overwrite
    public static void clearModelCache(IResourceManager newManager) {
        manager = newManager;
        stellar_core$ALIASES.clear();
        stellar_core$TEXTURES.clear();
        stellar_core$CACHE.clear();
        // putting the builtin models in
        stellar_core$CACHE.put(new ResourceLocation("minecraft:builtin/generated"), ItemLayerModel.INSTANCE);
        stellar_core$CACHE.put(new ResourceLocation("minecraft:block/builtin/generated"), ItemLayerModel.INSTANCE);
        stellar_core$CACHE.put(new ResourceLocation("minecraft:item/builtin/generated"), ItemLayerModel.INSTANCE);
    }

    /**
     * @author Kasumi_Nova
     * @reason Allow multithreaded model loading.
     */
    @Overwrite
    static Iterable<ResourceLocation> getTextures() {
        return stellar_core$TEXTURES;
    }

    @Redirect(method = "addAlias", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object redirectAddAlias(final Map<ResourceLocation, ResourceLocation> instance, final Object k, final Object v) {
        synchronized (stellar_core$ALIASES) {
            return stellar_core$ALIASES.put((ResourceLocation) k, (ResourceLocation) v);
        }
    }

    @Redirect(
            method = "getMissingModel(Lnet/minecraft/util/ResourceLocation;Ljava/lang/Throwable;)Lnet/minecraftforge/client/model/IModel;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;addAll(Ljava/util/Collection;)Z"
            )
    )
    private static boolean redirectGetMissingModel(final Set<ResourceLocation> instance, final Collection<ResourceLocation> es) {
        return stellar_core$TEXTURES.addAll(es);
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
