package github.kasuminova.stellarcore.mixin.minecraft.forge;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import github.kasuminova.stellarcore.common.util.StellarEnvironment;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;
import java.util.stream.Stream;

@Mixin(ASMDataTable.class)
public abstract class MixinASMDataTable {

    @Shadow(remap = false)
    private Map<ModContainer, SetMultimap<String, ASMDataTable.ASMData>> containerAnnotationData;

    @Shadow(remap = false)
    public abstract SetMultimap<String, ASMDataTable.ASMData> getAnnotationsFor(final ModContainer container);

    @Unique
    private final AtomicReference<Map<ModContainer, SetMultimap<String, ASMDataTable.ASMData>>> stellar_core$atomicRef = new AtomicReference<>(null);

    @Unique
    private final AtomicBoolean stellar_core$initializingData = new AtomicBoolean(false);

    /**
     * @author Kasumi_Nova
     * @reason 使用系统一半的 CPU 而不是全部 CPU，防止系统冻结。
     */
    @Inject(method = "getAnnotationsFor", at = @At("HEAD"), remap = false, cancellable = true)
    public void getAnnotationsFor(final ModContainer container, final CallbackInfoReturnable<SetMultimap<String, ASMDataTable.ASMData>> cir) {
        if (stellar_core$initializingData.get()) {
            return;
        }
        stellar_core$getAnnotationsForInternal(container, cir);
    }

    @Redirect(
            method = "getAnnotationsFor",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;collect(Ljava/util/stream/Collector;)Ljava/lang/Object;",
                    remap = false
            ),
            remap = false
    )
    private Object ensureInitialized(final Stream<Pair<ModContainer, ImmutableSetMultimap<String, ASMDataTable.ASMData>>> instance,
                                     final Collector<Pair<ModContainer, ImmutableSetMultimap<String, ASMDataTable.ASMData>>, ?, ImmutableMap<ModContainer, SetMultimap<String, ASMDataTable.ASMData>>> collector)
    {
        ImmutableMap<ModContainer, SetMultimap<String, ASMDataTable.ASMData>> result = instance.collect(collector);
        stellar_core$atomicRef.set(result);
        return result;
    }

    @Unique
    private void stellar_core$getAnnotationsForInternal(final ModContainer container, final CallbackInfoReturnable<SetMultimap<String, ASMDataTable.ASMData>> cir) {
        if (containerAnnotationData != null) {
            cir.setReturnValue(containerAnnotationData.get(container));
            return;
        }

        synchronized (ASMDataTable.class) {
            if (containerAnnotationData != null) {
                cir.setReturnValue(containerAnnotationData.get(container));
                return;
            }
            // 确保不会因为内存屏障丢失赋值。
            if (stellar_core$atomicRef.get() != null) {
                containerAnnotationData = stellar_core$atomicRef.get();
                cir.setReturnValue(containerAnnotationData.get(container));
            }
            stellar_core$initializeData(container);
        }

        cir.setReturnValue(containerAnnotationData.get(container));
    }

    @Unique
    private void stellar_core$initializeData(final ModContainer container) {
        stellar_core$initializingData.set(true);
        // 在自定义线程池提交会使 parallelStream 优先使用此线程池。
        ForkJoinPool pool = new ForkJoinPool(Math.max(2, StellarEnvironment.getConcurrency() / 2));
        pool.submit(() -> {
            getAnnotationsFor(container);
        }).join();
        pool.shutdown();
        stellar_core$initializingData.set(false);
    }

}
