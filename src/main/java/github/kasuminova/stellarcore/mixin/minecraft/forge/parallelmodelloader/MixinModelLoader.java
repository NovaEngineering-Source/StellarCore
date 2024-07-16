package github.kasuminova.stellarcore.mixin.minecraft.forge.parallelmodelloader;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.StellarCore;
import github.kasuminova.stellarcore.mixin.util.DefaultTextureGetter;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("SynchronizeOnNonFinalField")
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
    private Map<ModelBlockDefinition, IModel> loadingExceptions;

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

    @Shadow(remap = false)
    protected abstract void storeException(final ResourceLocation location, final Exception exception);

    @Shadow(remap = false)
    public static ModelResourceLocation getInventoryVariant(final String s) {
        return null;
    }

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

        Map<IModel, IBakedModel> bakedModelsConcurrent = new ConcurrentHashMap<>();
        models.keySet().stream().parallel().forEach((model) -> {
            String modelLocations = "[" + Joiner.on(", ").join(models.get(model)) + "]";
            synchronized (bakeBar) {
                bakeBar.step(modelLocations);
            }

            if (model == getMissingModel()) {
                bakedModelsConcurrent.put(model, missingBaked);
                return;
            }

            try {
                bakedModelsConcurrent.put(model, model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM, new DefaultTextureGetter()));
            } catch (Exception e) {
                FMLLog.log.error("Exception baking model for location(s) {}:", modelLocations, e);
                bakedModelsConcurrent.put(model, missingBaked);
            }
        });

        StellarCore.log.info("[StellarCore-MixinModelLoader] Baked {} models, took {}ms.", bakedModelsConcurrent.size(), System.currentTimeMillis() - startTime);
        bakedModels.putAll(bakedModelsConcurrent);
        return Collections.emptySet();
    }

    @Redirect(method = "loadBlocks", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<Object> stellar_core$injectLoadBlocks(
            List<Block> blocks,
            @Local(name = "blockBar") ProgressManager.ProgressBar blockBar,
            @Local(name = "mapper") BlockStateMapper mapper) {
        long startTime = System.currentTimeMillis();

        blocks.parallelStream().forEach(block -> {
            synchronized (blockBar) {
                blockBar.step(block.getRegistryName().toString());
            }
            Map<IBlockState, ModelResourceLocation> map = ((AccessorBlockStateMapper) mapper).getBlockStateMap();
            Object modelRL = map.get(block);
            if (modelRL != null) {
                synchronized (modelRL) {
                    Set<ResourceLocation> locations = mapper.getBlockstateLocations(block);
                    locations.parallelStream().forEach(location -> loadBlock(mapper, block, location));
                }
            } else {
                Set<ResourceLocation> locations = mapper.getBlockstateLocations(block);
                locations.parallelStream().forEach(location -> loadBlock(mapper, block, location));
            }
        });

        StellarCore.log.info("[StellarCore-MixinModelLoader] Loaded {} block models, took {}ms.", blocks.size(), System.currentTimeMillis() - startTime);
        return Collections.emptyIterator();
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
            List<Item> items,
            @Local(name = "itemBar") ProgressManager.ProgressBar itemBar) {
        if (!stellar_core$reflectInitialized) {
            stellar_core$initializeReflect();
        }

        long startTime = System.currentTimeMillis();

        items.parallelStream().forEach(item -> {
            synchronized (itemBar) {
                itemBar.step(item.getRegistryName().toString());
            }

            getVariantNames(item).parallelStream().forEach(s -> {
                ResourceLocation file = getItemLocation(s);
                ModelResourceLocation memory = getInventoryVariant(s);
                IModel model = getMissingModel();
                Exception exception = null;
                try {
                    model = ModelLoaderRegistry.getModel(memory);
                } catch (Exception blockstateException) {
                    try {
                        model = ModelLoaderRegistry.getModel(file);
                        synchronized (items) {
                            stellar_core$addAlias(memory, file);
                        }
                    } catch (Exception normalException) {
                        exception = stellar_core$createItemLoadingException("Could not load item model either from the normal location " + file + " or from the blockstate", normalException, blockstateException);
                    }
                }
                if (exception != null) {
                    synchronized (loadingExceptions) {
                        storeException(memory, exception);
                    }
                    model = stellar_core$getMissingModel(memory, exception);
                }
                stateModels.put(memory, model);
            });
        });

        StellarCore.log.info("[StellarCore-MixinModelLoader] Loaded {} items models, took {}ms.", items.size(), System.currentTimeMillis() - startTime);
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
    private static Method stellar_core$addAlias = null;
    @Unique
    private static Method stellar_core$getMissingModel = null;

    @Unique
    private static boolean stellar_core$reflectInitialized = false;

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
            stellar_core$addAlias.invoke(null, from, to);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private static IModel stellar_core$getMissingModel(ResourceLocation location, Throwable cause) {
        try {
            return (IModel) stellar_core$getMissingModel.invoke(null, location, cause);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private static void stellar_core$initializeReflect() {
        try {
            stellar_core$ItemLoadingException = Class.forName("net.minecraftforge.client.model.ModelLoader$ItemLoadingException");
            stellar_core$ItemLoadingExceptionConstructor = stellar_core$ItemLoadingException.getConstructor(String.class, Exception.class, Exception.class);
            stellar_core$ModelLoaderRegistry = Class.forName("net.minecraftforge.client.model.ModelLoaderRegistry");
            stellar_core$addAlias = stellar_core$ModelLoaderRegistry.getDeclaredMethod("addAlias", ResourceLocation.class, ResourceLocation.class);
            stellar_core$addAlias.setAccessible(true);
            stellar_core$ModelLoaderRegistry = Class.forName("net.minecraftforge.client.model.ModelLoaderRegistry");
            stellar_core$getMissingModel = stellar_core$ModelLoaderRegistry.getDeclaredMethod("getMissingModel", ResourceLocation.class, Throwable.class);
            stellar_core$getMissingModel.setAccessible(true);
        } catch (Throwable e) {
            // Always throws exception because it cannot be failure.
            throw new RuntimeException("[StellarCore-MixinModelLoader] Caught a fatal exception, please report to mod author!", e);
        }
        stellar_core$reflectInitialized = true;
    }

}
