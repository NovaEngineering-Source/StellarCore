package github.kasuminova.stellarcore.mixin.ctm;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import team.chisel.ctm.CTM;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.client.model.parsing.ModelLoaderCTM;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;
import team.chisel.ctm.client.util.ResourceUtil;
import team.chisel.ctm.client.util.TextureMetadataHandler;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("StaticVariableMayNotBeInitialized")
@Mixin(value = TextureMetadataHandler.class, remap = false)
public abstract class MixinTextureMetadataHandler {

    @Final
    @Shadow
    private Object2BooleanMap<ResourceLocation> wrappedModels;

    @Final
    @Shadow
    private static Class<?> vanillaModelWrapperClass;

    @Final
    @Shadow
    private static Field modelWrapperModel;

    @Final
    @Shadow
    private static Class<?> multipartModelClass;

    @Final
    @Shadow
    private static Field multipartPartModels;

    @Nonnull
    @Shadow
    protected abstract IBakedModel wrap(final IModel model, final IBakedModel object) throws IOException;

    /**
     * @author Kasumi_Nova
     * @reason Parallel loading
     */
    @SuppressWarnings({"SynchronizeOnNonFinalField", "deprecation"})
    @Overwrite
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onModelBake(ModelBakeEvent event) {
        Map<ModelResourceLocation, IModel> stateModels = ReflectionHelper.getPrivateValue(ModelLoader.class, event.getModelLoader(), "stateModels");
        event.getModelRegistry().getKeys().parallelStream().forEach(mrl -> {
            IModel rootModel = stateModels.get(mrl);
            if (rootModel == null || rootModel instanceof IModelCTM || ModelLoaderCTM.parsedLocations.contains(mrl)) {
                return;
            }

            Deque<ResourceLocation> dependencies = new ArrayDeque<>();
            Set<ResourceLocation> seenModels = new ObjectOpenHashSet<>();
            dependencies.push(mrl);
            seenModels.add(mrl);
            boolean shouldWrap;
            synchronized (wrappedModels) {
                shouldWrap = wrappedModels.getOrDefault(mrl, false);
            }
            // Breadth-first loop through dependencies, exiting as soon as a CTM texture is found, and skipping duplicates/cycles
            while (!shouldWrap && !dependencies.isEmpty()) {
                ResourceLocation dep = dependencies.pop();
                IModel model;
                try {
                    model = dep == mrl ? rootModel : ModelLoaderRegistry.getModel(dep);
                } catch (Exception e) {
                    continue;
                }

                Set<ResourceLocation> textures = new ObjectOpenHashSet<>(model.getTextures());
                // FORGE WHY
                if (vanillaModelWrapperClass.isAssignableFrom(model.getClass())) {
                    ModelBlock parent;
                    try {
                        parent = ((ModelBlock) modelWrapperModel.get(model)).parent;
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                    while (parent != null) {
                        textures.addAll(parent.textures.values().stream()
                                .filter(s -> !s.startsWith("#"))
                                .map(ResourceLocation::new)
                                .collect(Collectors.toSet())
                        );
                        parent = parent.parent;
                    }
                }

                Set<ResourceLocation> newDependencies = new ObjectOpenHashSet<>(model.getDependencies());

                // FORGE WHYYYYY
                if (multipartModelClass.isAssignableFrom(model.getClass())) {
                    Map<?, IModel> partModels;
                    try {
                        partModels = (Map<?, IModel>) multipartPartModels.get(model);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                    textures = partModels.values().stream()
                            .map(IModel::getTextures)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet());
                    newDependencies.addAll(partModels.values().stream()
                            .flatMap(m -> m.getDependencies().stream())
                            .collect(Collectors.toList()));
                }

                for (ResourceLocation tex : textures) {
                    IMetadataSectionCTM meta = null;
                    try {
                        meta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(tex));
                    } catch (IOException e) {} // Fallthrough
                    if (meta != null) {
                        shouldWrap = true;
                        break;
                    }
                }

                for (ResourceLocation newDependency : newDependencies) {
                    if (seenModels.add(newDependency)) {
                        dependencies.push(newDependency);
                    }
                }
            }

            synchronized (wrappedModels) {
                wrappedModels.put(mrl, shouldWrap);
            }

            if (shouldWrap) {
                try {
                    synchronized (event) {
                        event.getModelRegistry().putObject(mrl, wrap(rootModel, event.getModelRegistry().getObject(mrl)));
                    }
                    dependencies.clear();
                } catch (IOException e) {
                    CTM.logger.error("Could not wrap model " + mrl + ". Aborting...", e);
                }
            }
        });
    }

}
