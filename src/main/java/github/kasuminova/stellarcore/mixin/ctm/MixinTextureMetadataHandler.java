package github.kasuminova.stellarcore.mixin.ctm;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarEnvironment;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import github.kasuminova.stellarcore.shaded.org.jctools.queues.atomic.MpscLinkedAtomicQueue;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.chisel.ctm.CTM;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.client.model.parsing.ModelLoaderCTM;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;
import team.chisel.ctm.client.util.ResourceUtil;
import team.chisel.ctm.client.util.TextureMetadataHandler;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("StaticVariableMayNotBeInitialized")
@Mixin(value = TextureMetadataHandler.class, remap = false)
public abstract class MixinTextureMetadataHandler {

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

    @Unique
    private final Map<ResourceLocation, Boolean> stellar_core$wrappedModelsConcurrent = new NonBlockingHashMap<>();

    @Nonnull
    @Shadow
    protected abstract IBakedModel wrap(final IModel model, final IBakedModel object) throws IOException;

    /**
     * @author Kasumi_Nova
     * @reason Parallel loading
     */
    @SuppressWarnings("deprecation")
    @Inject(method = "onModelBake", at = @At("HEAD"), cancellable = true)
    public void onModelBake(final ModelBakeEvent event, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.ctm.textureMetadataHandler || !StellarEnvironment.shouldParallel()) {
            return;
        }
        ci.cancel();

        Map<ModelResourceLocation, IModel> stateModels = ReflectionHelper.getPrivateValue(ModelLoader.class, event.getModelLoader(), "stateModels");
        IRegistry<ModelResourceLocation, IBakedModel> registry = event.getModelRegistry();
        Queue<Tuple<ModelResourceLocation, IBakedModel>> wrappedConcurrent = new MpscLinkedAtomicQueue<>();

        try {
            registry.getKeys().parallelStream().forEach(mrl -> {
                IModel rootModel = stateModels.get(mrl);
                if (rootModel == null || rootModel instanceof IModelCTM || ModelLoaderCTM.parsedLocations.contains(mrl)) {
                    return;
                }

                Deque<ResourceLocation> dependencies = new ArrayDeque<>();
                Set<ResourceLocation> seenModels = new ObjectOpenHashSet<>();
                dependencies.push(mrl);
                seenModels.add(mrl);
                boolean shouldWrap = stellar_core$wrappedModelsConcurrent.getOrDefault(mrl, Boolean.FALSE);
                while (!shouldWrap && !dependencies.isEmpty()) {
                    ResourceLocation dep = dependencies.pop();
                    IModel model;
                    try {
                        model = dep == mrl ? rootModel : ModelLoaderRegistry.getModel(dep);
                    } catch (Exception e) {
                        continue;
                    }

                    Set<ResourceLocation> textures = new ObjectOpenHashSet<>(model.getTextures());
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
                    if (multipartModelClass.isAssignableFrom(model.getClass())) {
                        Map<?, IModel> partModels;
                        try {
                            partModels = (Map<?, IModel>) multipartPartModels.get(model);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                        textures = new ObjectOpenHashSet<>();
                        for (IModel partModel : partModels.values()) {
                            textures.addAll(partModel.getTextures());
                            newDependencies.addAll(partModel.getDependencies());
                        }
                    }

                    for (ResourceLocation tex : textures) {
                        IMetadataSectionCTM meta = null;
                        try {
                            meta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(tex));
                        } catch (IOException ignored) {
                        }
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

                stellar_core$wrappedModelsConcurrent.put(mrl, shouldWrap ? Boolean.TRUE : Boolean.FALSE);

                if (shouldWrap) {
                    try {
                        IBakedModel wrapped = wrap(rootModel, registry.getObject(mrl));
                        wrappedConcurrent.offer(new Tuple<>(mrl, wrapped));
                    } catch (IOException e) {
                        CTM.logger.error("[StellarCore-CTM] Could not wrap model {}. Aborting.", mrl, e);
                    }
                }
            });
            Tuple<ModelResourceLocation, IBakedModel> tuple;
            while ((tuple = wrappedConcurrent.poll()) != null) {
                registry.putObject(tuple.getFirst(), tuple.getSecond());
            }
        } finally {
            stellar_core$wrappedModelsConcurrent.clear();
        }
    }

}
