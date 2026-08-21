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
import java.util.Optional;
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

    @Unique
    private static Set<ResourceLocation> stellar_core$getTextures(
        final String operation,
        final ModelResourceLocation root,
        final ResourceLocation dependency,
        final IModel model) {
        try {
            return new ObjectOpenHashSet<>(model.getTextures());
        } catch (RuntimeException | Error e) {
            stellar_core$logScanFailure(operation, root, dependency, model, e);
            throw e;
        }
    }

    @Unique
    private static Set<ResourceLocation> stellar_core$getDependencies(
        final String operation,
        final ModelResourceLocation root,
        final ResourceLocation dependency,
        final IModel model) {
        try {
            return new ObjectOpenHashSet<>(model.getDependencies());
        } catch (RuntimeException | Error e) {
            stellar_core$logScanFailure(operation, root, dependency, model, e);
            throw e;
        }
    }

    @Unique
    private static void stellar_core$logScanFailure(
        final String operation,
        final ModelResourceLocation root,
        final ResourceLocation dependency,
        final IModel model,
        final Throwable failure) {
        final String modelClass = model == null ? "<unresolved>" : model.getClass().getName();
        final String modelIdentity = model == null ? "<unresolved>" : Integer.toHexString(System.identityHashCode(model));
        String parentLocation = "<not-a-vanilla-model>";
        String resolvedParent = "<not-a-vanilla-model>";

        if (model != null) {
            try {
                Optional<ModelBlock> vanillaModel = model.asVanillaModel();
                if (vanillaModel.isPresent()) {
                    ModelBlock modelBlock = vanillaModel.get();
                    parentLocation = String.valueOf(modelBlock.getParentLocation());
                    ModelBlock parent = modelBlock.parent;
                    resolvedParent = parent == null ? "<unresolved>" : parent.toString();
                }
            } catch (RuntimeException | Error diagnosticFailure) {
                parentLocation = "<diagnostics-failed:" + diagnosticFailure.getClass().getName() + ">";
                resolvedParent = parentLocation;
            }
        }

        CTM.logger.error(
            "[StellarCore-CTM] METADATA_SCAN_FAILURE operation={} root={} dependency={} modelClass={} modelIdentity={} parentLocation={} resolvedParent={} thread={}",
            operation,
            root,
            dependency,
            modelClass,
            modelIdentity,
            parentLocation,
            resolvedParent,
            Thread.currentThread().getName(),
            failure
        );
    }

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
            // Breadth-first loop through dependencies, exiting as soon as a CTM texture is found, and skipping duplicates/cycles
            while (!shouldWrap && !dependencies.isEmpty()) {
                ResourceLocation dep = dependencies.pop();
                IModel model;
                try {
                    model = dep == mrl ? rootModel : ModelLoaderRegistry.getModel(dep);
                } catch (Exception e) {
                    continue;
                } catch (Error e) {
                    stellar_core$logScanFailure("resolveDependency", mrl, dep, null, e);
                    throw e;
                }

                Set<ResourceLocation> textures = stellar_core$getTextures("getTextures", mrl, dep, model);
                // FORGE WHY
                if (vanillaModelWrapperClass.isAssignableFrom(model.getClass())) {
                    ModelBlock parent;
                    try {
                        parent = ((ModelBlock) modelWrapperModel.get(model)).parent;
                    } catch (IllegalAccessException e) {
                        stellar_core$logScanFailure("readVanillaParent", mrl, dep, model, e);
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

                Set<ResourceLocation> newDependencies = stellar_core$getDependencies("getDependencies", mrl, dep, model);

                // FORGE WHYYYYY
                if (multipartModelClass.isAssignableFrom(model.getClass())) {
                    Map<?, IModel> partModels;
                    try {
                        partModels = (Map<?, IModel>) multipartPartModels.get(model);
                    } catch (IllegalAccessException e) {
                        stellar_core$logScanFailure("readMultipartParts", mrl, dep, model, e);
                        throw new RuntimeException(e);
                    }
                    textures = new ObjectOpenHashSet<>();
                    for (Map.Entry<?, IModel> part : partModels.entrySet()) {
                        final String partOperation = "multipartPart=" + part.getKey();
                        final IModel partModel = part.getValue();
                        textures.addAll(stellar_core$getTextures(partOperation + ".getTextures", mrl, dep, partModel));
                        newDependencies.addAll(stellar_core$getDependencies(partOperation + ".getDependencies", mrl, dep, partModel));
                    }
                }

                for (ResourceLocation tex : textures) {
                    IMetadataSectionCTM meta = null;
                    try {
                        meta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(tex));
                    } catch (IOException e) {
                    } // Fallthrough
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
                    dependencies.clear();
                } catch (IOException e) {
                    CTM.logger.error("[StellarCore-CTM] METADATA_SCAN_WRAP_FAILED root={} thread={}", mrl, Thread.currentThread().getName(), e);
                } catch (RuntimeException | Error e) {
                    stellar_core$logScanFailure("wrap", mrl, mrl, rootModel, e);
                    throw e;
                }
            }
        });
        Tuple<ModelResourceLocation, IBakedModel> tuple;
        while ((tuple = wrappedConcurrent.poll()) != null) {
            registry.putObject(tuple.getFirst(), tuple.getSecond());
        }
        stellar_core$wrappedModelsConcurrent.clear();
    }

}
