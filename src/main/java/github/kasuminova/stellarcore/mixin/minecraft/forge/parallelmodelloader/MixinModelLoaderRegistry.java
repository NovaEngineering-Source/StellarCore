package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import com.google.common.base.Joiner;
import github.kasuminova.stellarcore.client.model.AsyncUnsafeModels;
import github.kasuminova.stellarcore.client.model.ModelLoaderRegistryRef;
import github.kasuminova.stellarcore.client.model.ParallelModelLoaderAsyncBlackList;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.ConcurrentModelLoaderRegistry;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings({"StaticVariableMayNotBeInitialized", "SynchronizeOnNonFinalField"})
@Mixin(value = ModelLoaderRegistry.class, remap = false)
public abstract class MixinModelLoaderRegistry implements ConcurrentModelLoaderRegistry {

    @Unique
    private static final ThreadLocal<ObjectArrayList<ResourceLocation>> stellar_core$LOADING_MODELS = ThreadLocal.withInitial(ObjectArrayList::new);

    @Unique
    private static volatile ICustomModelLoader[] stellar_core$loaderArray = new ICustomModelLoader[0];

    @Unique
    private static final AtomicInteger stellar_core$ACTIVE_MODEL_LOADS = new AtomicInteger();

    @Unique
    private static Map<ResourceLocation, IModel> stellar_core$cache = new NonBlockingHashMap<>();

    @Unique
    private static Map<ResourceLocation, ResourceLocation> stellar_core$aliases = new NonBlockingHashMap<>();

    @Unique
    private static Set<ResourceLocation> stellar_core$textures = new NonBlockingHashSet<>();

    @Unique
    private static volatile boolean stellar_core$concurrent = false;

    @Final
    @Shadow
    private static Map<ResourceLocation, IModel> cache;

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

    @Shadow
    public static IModel getModel(final ResourceLocation location) {
        return null;
    }

    @Inject(method = "registerLoader", at = @At("RETURN"), remap = false)
    private static void injectRegisterLoader(final ICustomModelLoader loader, final CallbackInfo ci) {
        stellar_core$loaderArray = loaders.toArray(new ICustomModelLoader[0]);
        Class<? extends ICustomModelLoader> loaderClass = loader.getClass();
        StellarLog.LOG.info("[StellarCore-ParallelModelLoader] Registered model loader: {}, AsyncBlackListed: {}",
                loaderClass.getName(),
                ParallelModelLoaderAsyncBlackList.INSTANCE.isInSet(loaderClass)
        );
    }

    /**
     * @author Kasumi_Nova
     * @reason Allow multithreaded model loading.
     */
    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getModel(final ResourceLocation location, final CallbackInfoReturnable<IModel> cir) throws Exception {
        IModel model;
        IModel cached = stellar_core$cache.get(location);
        if (cached != null) {
            cir.setReturnValue(cached);
            return;
        }

        final ObjectArrayList<ResourceLocation> loadingModels = stellar_core$LOADING_MODELS.get();
        for (int i = 0; i < loadingModels.size(); i++) {
            final ResourceLocation loading = loadingModels.get(i);
            if (location.getClass() == loading.getClass() && location.equals(loading)) {
                throw new ModelLoaderRegistry.LoaderException("circular model dependencies, stack: [" + Joiner.on(", ").join(loadingModels) + "]");
            }
        }
        loadingModels.add(location);
        stellar_core$ACTIVE_MODEL_LOADS.incrementAndGet();
        try {
            ResourceLocation aliased = stellar_core$aliases.get(location);
            if (aliased != null) {
                cir.setReturnValue(getModel(aliased));
                return;
            }

            if (!stellar_core$concurrent) {
                StellarLog.LOG.warn("[StellarCore-ParallelModelLoader] A mod trying to load model `{}` without concurrent state, it may cause some performance issues.", location);
            }

            ResourceLocation actual = getActualLocation(location);
            ICustomModelLoader accepted = null;
            final ICustomModelLoader[] registeredLoaders = stellar_core$loaderArray;
            for (final ICustomModelLoader loader : registeredLoaders) {
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
            final boolean asyncUnsafe = ParallelModelLoaderAsyncBlackList.INSTANCE.isInSet(accepted.getClass());
            try {
                if (asyncUnsafe) {
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
            if (asyncUnsafe) {
                AsyncUnsafeModels.register(model);
            }
            if (!stellar_core$concurrent) {
                synchronized (stellar_core$textures) {
                    stellar_core$textures.addAll(model.getTextures());
                }
            } else if (asyncUnsafe) {
                synchronized (accepted) {
                    stellar_core$textures.addAll(model.getTextures());
                }
            } else {
                stellar_core$textures.addAll(model.getTextures());
            }
        } finally {
            try {
                ResourceLocation popLoc = loadingModels.pop();
                if (popLoc != location) {
                    throw new IllegalStateException("Corrupted loading model stack: " + popLoc + " != " + location);
                }
            } finally {
                stellar_core$ACTIVE_MODEL_LOADS.decrementAndGet();
            }
        }

        if (!stellar_core$concurrent) {
            synchronized (stellar_core$cache) {
                stellar_core$cache.put(location, model);
            }
        } else {
            stellar_core$cache.put(location, model);
        }
        for (ResourceLocation dep : model.getDependencies()) {
            getModelOrMissing(dep);
        }
        cir.setReturnValue(model);
    }

    /**
     * @author Kasumi_Nova
     * @reason Allow multithreaded model loading.
     */
    @Overwrite
    public static boolean loaded(ResourceLocation location) {
        return stellar_core$cache.containsKey(location);
    }

    /**
     * @author Kasumi_Nova
     * @reason Allow multithreaded model loading.
     */
    @Overwrite
    @SuppressWarnings("InstantiationOfUtilityClass")
    public static void clearModelCache(IResourceManager newManager) {
        manager = newManager;
        ModelLoaderRegistryRef.instance = (ConcurrentModelLoaderRegistry) new ModelLoaderRegistry();
        AsyncUnsafeModels.clear();
        stellar_core$aliases.clear();
        stellar_core$textures.clear();
        stellar_core$cache.clear();
        // putting the builtin models in
        stellar_core$cache.put(new ResourceLocation("minecraft:builtin/generated"), ItemLayerModel.INSTANCE);
        stellar_core$cache.put(new ResourceLocation("minecraft:block/builtin/generated"), ItemLayerModel.INSTANCE);
        stellar_core$cache.put(new ResourceLocation("minecraft:item/builtin/generated"), ItemLayerModel.INSTANCE);
    }

    /**
     * @author Kasumi_Nova
     * @reason Allow multithreaded model loading.
     */
    @Overwrite
    static Iterable<ResourceLocation> getTextures() {
        if (stellar_core$concurrent && stellar_core$LOADING_MODELS.get().isEmpty()) {
            stellar_core$awaitNoActiveModelLoads();
        }
        return stellar_core$textures;
    }

    @Unique
    private static void stellar_core$awaitNoActiveModelLoads() {
        int active = stellar_core$ACTIVE_MODEL_LOADS.get();
        if (active <= 0) {
            return;
        }

        final long deadlineNanos = System.nanoTime() + 60_000_000_000L; // 60s
        while ((active = stellar_core$ACTIVE_MODEL_LOADS.get()) > 0 && System.nanoTime() < deadlineNanos) {
            LockSupport.parkNanos(1_000_000L); // 1ms
        }

        if (active > 0) {
            StellarLog.LOG.warn("[StellarCore-ParallelModelLoader] Timed out waiting for {} active model loads; stitching may miss sprites.", active);
        }
    }

    @Redirect(method = "addAlias", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object redirectAddAlias(final Map<ResourceLocation, ResourceLocation> instance, final Object k, final Object v) {
        if (stellar_core$concurrent) {
            return stellar_core$aliases.put((ResourceLocation) k, (ResourceLocation) v);
        } else {
            synchronized (stellar_core$aliases) {
                return stellar_core$aliases.put((ResourceLocation) k, (ResourceLocation) v);
            }
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
        if (stellar_core$concurrent) {
            return stellar_core$textures.addAll(es);
        } else {
            synchronized (stellar_core$textures) {
                return stellar_core$textures.addAll(es);
            }
        }
    }

    @Override
    public void stellar_core$toConcurrent() {
        if (stellar_core$concurrent) {
            return;
        }
        stellar_core$cache = new NonBlockingHashMap<>(stellar_core$cache);
        stellar_core$aliases = new NonBlockingHashMap<>(stellar_core$aliases);
        Set<ResourceLocation> oldTextures = stellar_core$textures;
        stellar_core$textures = new NonBlockingHashSet<>();
        stellar_core$textures.addAll(oldTextures);
        stellar_core$concurrent = true;
    }

    /**
     * 用于其他优化模组优化缓存，例如 FoamFix。
     */
    @Override
    public void stellar_core$writeToOriginalMap() {
        for (final Map.Entry<ResourceLocation, IModel> entry : stellar_core$cache.entrySet()) {
            //noinspection UseBulkOperation
            cache.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void stellar_core$toDefault() {
        if (!stellar_core$concurrent) {
            return;
        }
        if (StellarCoreConfig.PERFORMANCE.vanilla.wipeModelCache) {
            long startTime = System.currentTimeMillis();
            int removed = stellar_core$wipeCache();
            StellarLog.LOG.info("[StellarCore-ParallelModelLoader] Removed {} (Before: {}) model cache, took {}ms.", 
                    removed, cache.size(), System.currentTimeMillis() - startTime
            );
            stellar_core$cache = stellar_core$copyToDefault(stellar_core$cache);
        } else {
            stellar_core$cache = stellar_core$copyToDefault(cache);
        }
        cache.clear();
        AsyncUnsafeModels.clear();
        stellar_core$aliases = stellar_core$copyToDefault(stellar_core$aliases);
        stellar_core$textures = new ObjectOpenHashSet<>();
        stellar_core$concurrent = false;
    }

    @Unique
    private static <K, V> Map<K, V> stellar_core$copyToDefault(final Map<K, V> source) {
        final Map<K, V> copy = new Object2ObjectOpenHashMap<>(source.size());
        for (final Map.Entry<K, V> entry : source.entrySet()) {
            //noinspection UseBulkOperation
            copy.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    @Unique
    private static int stellar_core$wipeCache() {
        AtomicInteger removeCount = new AtomicInteger();
        ReferenceOpenHashSet<ResourceLocation> tmpSet = new ReferenceOpenHashSet<>(stellar_core$cache.keySet());
        (tmpSet.size() > 15_000 ? tmpSet.parallelStream() : tmpSet.stream())
                .filter(MixinModelLoaderRegistry::stellar_core$shouldRemove)
                .forEach(key -> {
                    stellar_core$cache.remove(key);
                    removeCount.incrementAndGet();
                });
        return removeCount.get();
    }

    @Unique
    private static boolean stellar_core$shouldRemove(final ResourceLocation key) {
        String namespace = key.getNamespace();
        if ("minecraft".equals(namespace) || "fml".equals(namespace) || "forge".equals(namespace)) {
            return false;
        }
        String path = key.getPath();
        return !path.endsWith("/generated") && !path.startsWith("builtin/");
    }

    @Unique
    private static volatile ICustomModelLoader stellar_core$variantLoader = null;

    @Unique
    private static volatile ICustomModelLoader stellar_core$vanillaLoader = null;

    @Unique
    private static ICustomModelLoader stellar_core$getVariantLoader() {
        ICustomModelLoader loader = stellar_core$variantLoader;
        if (loader == null) {
            loader = stellar_core$resolveLoader("net.minecraftforge.client.model.ModelLoader$VariantLoader");
            stellar_core$variantLoader = loader;
        }
        return loader;
    }

    @Unique
    private static ICustomModelLoader stellar_core$getVanillaLoader() {
        ICustomModelLoader loader = stellar_core$vanillaLoader;
        if (loader == null) {
            loader = stellar_core$resolveLoader("net.minecraftforge.client.model.ModelLoader$VanillaLoader");
            stellar_core$vanillaLoader = loader;
        }
        return loader;
    }

    @Unique
    private static ICustomModelLoader stellar_core$resolveLoader(final String className) {
        try {
            return (ICustomModelLoader) Class.forName(className).getEnumConstants()[0];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
