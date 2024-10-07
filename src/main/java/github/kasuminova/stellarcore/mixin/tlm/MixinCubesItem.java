package github.kasuminova.stellarcore.mixin.tlm;

import com.github.tartaricacid.touhoulittlemaid.client.model.pojo.CubesItem;
import github.kasuminova.stellarcore.client.pool.TLMCubesItemPool;
import github.kasuminova.stellarcore.mixin.util.CanonicalizationCubesItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.List;

@Mixin(value = CubesItem.class, remap = false)
public class MixinCubesItem implements CanonicalizationCubesItem {

    @Unique
    private static MethodHandle stellar_core$getRotationHandle = null;

    @Unique
    private static MethodHandle stellar_core$getPivotHandle = null;

    @Unique
    private static MethodHandle stellar_core$setRotationHandle = null;

    @Unique
    private static MethodHandle stellar_core$setPivotHandle = null;

    @Unique
    private static boolean stellar_core$handleInitialized = false;

    @Shadow
    private List<Float> uv;

    @Shadow
    private List<Float> size;

    @Shadow
    private List<Float> origin;

    @Unique
    @Override
    public void stellar_core$canonicalize() {
        TLMCubesItemPool.INSTANCE.canonicalizeAsync(uv, canonicalizedUV -> uv = canonicalizedUV);
        TLMCubesItemPool.INSTANCE.canonicalizeAsync(size, canonicalizedSize -> size = canonicalizedSize);
        TLMCubesItemPool.INSTANCE.canonicalizeAsync(origin, canonicalizedOrigin -> origin = canonicalizedOrigin);
        // Ver 1.2.5
        stellar_core$canonicalizeRotation();
        stellar_core$canonicalizePivot();
    }

    @Unique
    @SuppressWarnings({"DataFlowIssue", "unchecked"})
    private void stellar_core$canonicalizeRotation() {
        stellar_core$ensureHandleInitialized();
        if (stellar_core$getRotationHandle == null || stellar_core$setRotationHandle == null) {
            return;
        }
        try {
            List<Float> rotation = (List<Float>) stellar_core$getRotationHandle.invoke((CubesItem) (Object) this);
            TLMCubesItemPool.INSTANCE.canonicalizeAsync(rotation, canonicalizedRotation -> {
                try {
                    stellar_core$setRotationHandle.invoke((CubesItem) (Object) this, canonicalizedRotation);
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }

    @Unique
    @SuppressWarnings("DataFlowIssue")
    private void stellar_core$canonicalizePivot() {
        stellar_core$ensureHandleInitialized();
        if (stellar_core$getPivotHandle == null || stellar_core$setPivotHandle == null) {
            return;
        }
        try {
            List<Float> pivot = (List<Float>) stellar_core$getPivotHandle.invoke((CubesItem) (Object) this);
            TLMCubesItemPool.INSTANCE.canonicalizeAsync(pivot, canonicalizedPivot -> {
                try {
                    stellar_core$setPivotHandle.invoke((CubesItem) (Object) this, canonicalizedPivot);
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static void stellar_core$ensureHandleInitialized() {
        if (!stellar_core$handleInitialized) {
            try {
                Field rotation = CubesItem.class.getDeclaredField("rotation");
                rotation.setAccessible(true);
                stellar_core$getRotationHandle = MethodHandles.lookup().unreflectGetter(rotation);
                stellar_core$setRotationHandle = MethodHandles.lookup().unreflectSetter(rotation);
                Field pivot = CubesItem.class.getDeclaredField("pivot");
                pivot.setAccessible(true);
                stellar_core$getPivotHandle = MethodHandles.lookup().unreflectGetter(pivot);
                stellar_core$setPivotHandle = MethodHandles.lookup().unreflectSetter(pivot);
            } catch (Throwable e) {
            }
            stellar_core$handleInitialized = true;
        }
    }

}
