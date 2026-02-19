package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.client.integration.railcraft.RCModelBaker;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.ClassUtils;
import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.DefaultTextureGetter;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.ProgressManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"SynchronizeOnNonFinalField", "MethodMayBeStatic"})
@Mixin(ModelLoader.class)
public abstract class MixinModelLoader extends ModelBakery {

    @Final
    @Mutable
    @Shadow(remap = false)
    private Map<ModelResourceLocation, IModel> stateModels;

    @Final
    @Mutable
    @Shadow(remap = false)
    private Map<ModelResourceLocation, ModelBlockDefinition> multipartDefinitions;

    @Final
    @Mutable
    @Shadow(remap = false)
    private Map<ModelBlockDefinition, IModel> multipartModels;

    @Final
    @Mutable
    @Shadow(remap = false)
    private Map<ResourceLocation, Exception> loadingExceptions;

    @Unique
    private boolean stellar_core$concurrent = true;

    @SuppressWarnings("DataFlowIssue")
    public MixinModelLoader() {
        super(null, null, null);
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void injectInit(final IResourceManager resourceManagerIn, final TextureMap textureMapIn, final BlockModelShapes blockModelShapesIn, final CallbackInfo ci) {
        stateModels = new ConcurrentHashMap<>();
        multipartDefinitions = new ConcurrentHashMap<>();
        multipartModels = new ConcurrentHashMap<>();
        loadingExceptions = new ConcurrentHashMap<>();
    }

    @Shadow(remap = false)
    protected abstract IModel getMissingModel();

    @Redirect(method = "setupModelRegistry",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/HashMultimap;keySet()Ljava/util/Set;",
                    ordinal = 1,
                    remap = false
            )
    )
    private Set stellar_core$injectSetupModelRegistry(
            final HashMultimap<IModel, ModelResourceLocation> instance,
            @Local(name = "bakedModels") Map<IModel, IBakedModel> bakedModels,
            @Local(name = "models") HashMultimap<IModel, ModelResourceLocation> models,
            @Local(name = "bakeBar") ProgressManager.ProgressBar bakeBar,
            @Local(name = "missingBaked") IBakedModel missingBaked) {
        long startTime = System.currentTimeMillis();

        Map<IModel, IBakedModel> bakedModelsConcurrent = new NonBlockingHashMap<>();
        DefaultTextureGetter textureGetter = new DefaultTextureGetter();
        models.keySet().stream().parallel().forEach((model) -> {
            Set<ModelResourceLocation> locations = models.get(model);
            String modelLocations = "[" + Joiner.on(", ").join(locations) + "]";
            synchronized (bakeBar) {
                bakeBar.step(modelLocations);
            }

            if (model == getMissingModel()) {
                bakedModelsConcurrent.put(model, missingBaked);
                return;
            }

            try {
                IBakedModel loaded = RCModelBaker.load(locations, model, textureGetter);
                if (loaded != null) {
                    bakedModelsConcurrent.put(model, loaded);
                    return;
                }
                bakedModelsConcurrent.put(model, model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM, textureGetter));
            } catch (Exception e) {
                if (!StellarCoreConfig.FEATURES.vanilla.shutUpModelLoader) {
                    FMLLog.log.error("Exception baking model for location(s) {}:", modelLocations, e);
                }
                bakedModelsConcurrent.put(model, missingBaked);
            }
        });

        StellarLog.LOG.info("[StellarCore-ParallelModelLoader] Baked {} models, took {}ms.", bakedModelsConcurrent.size(), System.currentTimeMillis() - startTime);
        bakedModels.putAll(bakedModelsConcurrent);
        return Collections.emptySet();
    }

    @Redirect(method = "loadBlocks", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<Object> stellar_core$injectLoadBlocks(
            List<Block> blocks,
            @Local(name = "blockBar") ProgressManager.ProgressBar blockBar,
            @Local(name = "mapper") BlockStateMapper mapper) {
        long startTime = System.currentTimeMillis();
        stellar_core$toConcurrent();

        blocks.parallelStream().forEach(block -> {
            synchronized (blockBar) {
                blockBar.step(block.getRegistryName().toString());
            }

            IStateMapper stateMapper = ((AccessorBlockStateMapper) mapper).stellar_core$getBlockStateMap().get(block);
            if (stateMapper != null) {
                synchronized (stateMapper) {
                    for (ResourceLocation location : mapper.getBlockstateLocations(block)) {
                        loadBlock(mapper, block, location);
                    }
                }
                return;
            }

            for (ResourceLocation location : mapper.getBlockstateLocations(block)) {
                loadBlock(mapper, block, location);
            }
        });

        stellar_core$toDefault();
        StellarLog.LOG.info("[StellarCore-ParallelModelLoader] Loaded {} block models, took {}ms.", blocks.size(), System.currentTimeMillis() - startTime);
        return Collections.emptyIterator();
    }

    @Unique
    private void stellar_core$toConcurrent() {
        if (!stellar_core$concurrent) {
            stateModels = new ConcurrentHashMap<>(stateModels);
            multipartDefinitions = new ConcurrentHashMap<>(multipartDefinitions);
            multipartModels = new ConcurrentHashMap<>(multipartModels);
            loadingExceptions = new ConcurrentHashMap<>(loadingExceptions);
            stellar_core$concurrent = true;
        }
    }

    @Unique
    private void stellar_core$toDefault() {
        stateModels = new Object2ObjectOpenHashMap<>(stateModels);
        multipartDefinitions = new Object2ObjectOpenHashMap<>(multipartDefinitions);
        multipartModels = new Object2ObjectOpenHashMap<>(multipartModels);
        loadingExceptions = new Object2ObjectOpenHashMap<>(loadingExceptions);
        stellar_core$concurrent = false;
    }

    @Redirect(
            method = "loadItemModels",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;iterator()Ljava/util/Iterator;",
                    ordinal = 0
            )
    )
    private Iterator<Object> stellar_core$injectLoadItemModels(
            final List<Item> items,
            @Local(name = "itemBar") final ProgressManager.ProgressBar itemBar) {
        stellar_core$toConcurrent();
        stellar_core$ensureReflectInitialized();

        final IModel missingModel = ModelLoaderRegistry.getMissingModel();

        long startTime = System.currentTimeMillis();

        items.parallelStream().forEach(item -> {
            synchronized (itemBar) {
                itemBar.step(item.getRegistryName().toString());
            }

            for (String s : getVariantNames(item)) {
                ResourceLocation file = getItemLocation(s);
                ModelResourceLocation memory = ModelLoader.getInventoryVariant(s);
                IModel model = missingModel;
                Exception exception = null;
                try {
                    model = ModelLoaderRegistry.getModel(memory);
                } catch (Exception blockstateException) {
                    try {
                        model = ModelLoaderRegistry.getModel(file);
                        stellar_core$addAlias(memory, file);
                    } catch (Exception normalException) {
                        exception = stellar_core$createItemLoadingException(
                                "Could not load item model either from the normal location " + file + " or from the blockstate",
                                normalException,
                                blockstateException
                        );
                    }
                }
                if (exception != null) {
                    if (!StellarCoreConfig.FEATURES.vanilla.shutUpModelLoader) {
                        loadingExceptions.put(memory, exception);
                    }
                    model = stellar_core$getMissingModel(memory, exception);
                }
                stateModels.put(memory, model);
            }
        });

        stellar_core$toDefault();
        StellarLog.LOG.info("[StellarCore-ParallelModelLoader] Loaded {} items models, took {}ms.", items.size(), System.currentTimeMillis() - startTime);
        return Collections.emptyIterator();
    }

    // Reflection. So many magic fields...

    @Unique
    private static Class<?> stellar_core$ItemLoadingException = null;
    @Unique
    private static Constructor<?> stellar_core$ItemLoadingExceptionConstructor = null;

    @Unique
    private static Class<?> stellar_core$ModelLoaderRegistry = null;

    @Unique
    private static MethodHandle stellar_core$addAlias = null;
    @Unique
    private static MethodHandle stellar_core$getMissingModel = null;

    @Unique
    private static volatile boolean stellar_core$reflectInitialized = false;

    @Unique
    private static Exception stellar_core$createItemLoadingException(final String message, final Exception normalException, final Exception blockstateException) {
        try {
            return (Exception) stellar_core$ItemLoadingExceptionConstructor.newInstance(message, normalException, blockstateException);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private static void stellar_core$addAlias(ResourceLocation from, ResourceLocation to) {
        try {
            stellar_core$addAlias.invoke(from, to);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private static IModel stellar_core$getMissingModel(ResourceLocation location, Throwable cause) {
        try {
            return (IModel) stellar_core$getMissingModel.invoke(location, cause);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private static void stellar_core$ensureReflectInitialized() {
        if (stellar_core$reflectInitialized) {
            return;
        }
        synchronized (MixinModelLoader.class) {
            if (stellar_core$reflectInitialized) {
                return;
            }
            stellar_core$initializeReflect();
            stellar_core$reflectInitialized = true;
        }
    }

    @Unique
    private static void stellar_core$initializeReflect() {
        try {
            stellar_core$ItemLoadingException = Class.forName("net.minecraftforge.client.model.ModelLoader$ItemLoadingException");
            stellar_core$ItemLoadingExceptionConstructor = stellar_core$ItemLoadingException.getConstructor(String.class, Exception.class, Exception.class);
            stellar_core$ModelLoaderRegistry = Class.forName("net.minecraftforge.client.model.ModelLoaderRegistry");
            Method addAlias = stellar_core$ModelLoaderRegistry.getDeclaredMethod("addAlias", ResourceLocation.class, ResourceLocation.class);
            addAlias.setAccessible(true);
            stellar_core$addAlias = MethodHandles.lookup().unreflect(addAlias);
            stellar_core$ModelLoaderRegistry = Class.forName("net.minecraftforge.client.model.ModelLoaderRegistry");
            Method getMissingModel = stellar_core$ModelLoaderRegistry.getDeclaredMethod("getMissingModel", ResourceLocation.class, Throwable.class);
            getMissingModel.setAccessible(true);
            stellar_core$getMissingModel = MethodHandles.lookup().unreflect(getMissingModel);
        } catch (Throwable e) {
            // Always throws exception because it cannot be failure.
            throw new RuntimeException("[StellarCore-ParallelModelLoader] Caught a fatal exception, please report to mod author!", e);
        }
    }

}
