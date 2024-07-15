package github.kasuminova.stellarcore.mixin.minecraft.forge;

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
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.ProgressManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

}
