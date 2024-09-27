package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import com.google.common.base.Joiner;
import github.kasuminova.stellarcore.client.model.ModelLoaderRegistryRef;
import github.kasuminova.stellarcore.client.model.ParallelModelLoaderAsyncBlackList;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.ConcurrentModelLoaderRegistry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"StaticVariableMayNotBeInitialized", "SynchronizeOnNonFinalField"})
@Mixin(value = ModelLoaderRegistry.class, remap = false)
public abstract class MixinModelLoaderRegistry implements ConcurrentModelLoaderRegistry {

    @Unique
    private static final ThreadLocal<Deque<ResourceLocation>> stellar_core$LOADING_MODELS = ThreadLocal.withInitial(ArrayDeque::new);

    @Unique
    private static Map<ResourceLocation, IModel> stellar_core$cache = new ConcurrentHashMap<>();

    @Unique
    private static Map<ResourceLocation, ResourceLocation> stellar_core$aliases = new ConcurrentHashMap<>();

    @Unique
    private static Set<ResourceLocation> stellar_core$textures = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Unique
    private static boolean stellar_core$concurrent = false;

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
        Class<? extends ICustomModelLoader> loaderClass = loader.getClass();
        StellarLog.LOG.info("[StellarCore-ParallelModelLoader] Registered model loader: {}, AsyncBlackListed: {}",
                loaderClass.getName(),
                ParallelModelLoaderAsyncBlackList.INSTANCE.isInBlackList(loaderClass)
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

        for (ResourceLocation loading : stellar_core$LOADING_MODELS.get()) {
            if (location.getClass() == loading.getClass() && location.equals(loading)) {
                throw new ModelLoaderRegistry.LoaderException("circular model dependencies, stack: [" + Joiner.on(", ").join(stellar_core$LOADING_MODELS.get()) + "]");
            }
        }
        stellar_core$LOADING_MODELS.get().addLast(location);
        try {
            synchronized (stellar_core$aliases) {
                ResourceLocation aliased = stellar_core$aliases.get(location);
                if (aliased != null) {
                    cir.setReturnValue(getModel(aliased));
                    return;
                }
            }

            if (!stellar_core$concurrent) {
                StellarLog.LOG.warn("[StellarCore-ParallelModelLoader] A mod trying to load model `{}` without concurrent state, it may cause some performance issues.", location);
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
            if (!stellar_core$concurrent) {
                synchronized (stellar_core$textures) {
                    stellar_core$textures.addAll(model.getTextures());
                }
            } else {
                stellar_core$textures.addAll(model.getTextures());
            }
        } finally {
            ResourceLocation popLoc = stellar_core$LOADING_MODELS.get().removeLast();
            if (popLoc != location) {
                throw new IllegalStateException("Corrupted loading model stack: " + popLoc + " != " + location);
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
        return stellar_core$textures;
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
            synchronized (stellar_core$aliases) {
                return stellar_core$textures.addAll(es);
            }
        }
    }

    @Override
    public void stellar_core$toConcurrent() {
        if (stellar_core$concurrent) {
            return;
        }
        stellar_core$cache = new ConcurrentHashMap<>(stellar_core$cache);
        stellar_core$aliases = new ConcurrentHashMap<>(stellar_core$aliases);
        stellar_core$textures = Collections.newSetFromMap(new ConcurrentHashMap<>());
        stellar_core$concurrent = true;
    }

    /**
     * 用于其他优化模组优化缓存，例如 FoamFix。
     */
    @Override
    public void stellar_core$writeToOriginalMap() {
        cache.putAll(stellar_core$cache);
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
            stellar_core$cache = new Object2ObjectOpenHashMap<>(stellar_core$cache);
        } else {
            stellar_core$cache = new Object2ObjectOpenHashMap<>(cache);
        }
        cache.clear();
        stellar_core$aliases = new Object2ObjectOpenHashMap<>(stellar_core$aliases);
        stellar_core$textures = new ObjectOpenHashSet<>();
        stellar_core$concurrent = false;
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
