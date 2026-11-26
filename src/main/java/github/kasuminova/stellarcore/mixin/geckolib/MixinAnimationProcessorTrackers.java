package github.kasuminova.stellarcore.mixin.geckolib;

import github.kasuminova.stellarcore.client.animation.AnimationTrackerAccess;
import github.kasuminova.stellarcore.client.animation.AnimationTrackerFrames;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib3.core.processor.AnimationProcessor;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.core.snapshot.DirtyTracker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(value = AnimationProcessor.class, remap = false)
public abstract class MixinAnimationProcessorTrackers implements AnimationTrackerAccess {
    // The redirected operations use the primitive set; NEW still requires a non-null legacy instance.
    @Unique
    private static final HashSet<?> stellar_core$unusedLegacySet = new HashSet<>(0);
    @Unique
    private final AnimationTrackerFrames.Pool stellar_core$trackerPool = new AnimationTrackerFrames.Pool();
    @Unique
    private final IntSet stellar_core$animatedIds = new IntOpenHashSet(2);
    @Shadow
    private List<IBone> modelRendererList;
    @Unique
    private boolean stellar_core$animatedNullId;

    // The original private set is unused after all three tickAnimation operations are redirected.
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "()Ljava/util/HashSet;"), require = 1, allow = 1)
    private HashSet<?> stellar_core$omitBoxedAnimatedIds() {
        return stellar_core$unusedLegacySet;
    }

    @Redirect(method = "tickAnimation", at = @At(value = "INVOKE", target = "Ljava/util/Set;clear()V"), require = 1, allow = 1)
    private void stellar_core$clearAnimatedIds(final Set<?> ignored) {
        stellar_core$animatedIds.clear();
        stellar_core$animatedNullId = false;
    }

    @Redirect(method = "tickAnimation", at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"), require = 1, allow = 1)
    private boolean stellar_core$containsAnimatedId(final Set<?> ignored, final Object value) {
        return value == null ? stellar_core$animatedNullId : stellar_core$animatedIds.contains(((Integer) value).intValue());
    }

    @Redirect(method = "tickAnimation", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"), require = 1, allow = 1)
    private boolean stellar_core$addAnimatedId(final Set<?> ignored, final Object value) {
        if (value != null) return stellar_core$animatedIds.add(((Integer) value).intValue());
        final boolean added = !stellar_core$animatedNullId;
        stellar_core$animatedNullId = true;
        return added;
    }

    @Override
    public AnimationTrackerFrames.Pool stellar_core$trackerPool() {
        return stellar_core$trackerPool;
    }

    /**
     * @author StellarCore
     * @reason Reuse dirty trackers only inside a scoped animation call; preserve independent fallback calls.
     */
    @Overwrite
    private HashMap<String, DirtyTracker> createNewDirtyTracker() {
        final AnimationTrackerFrames.Frame frame = AnimationTrackerFrames.current((AnimationProcessor<?>) (Object) this);
        return frame == null ? AnimationTrackerFrames.fresh(modelRendererList) : frame.prepare(modelRendererList);
    }

    /**
     * @author StellarCore
     * @reason Preserve first-match lookup without allocating a stream and predicate for each bone.
     */
    @Overwrite
    public IBone getBone(final String name) {
        for (int index = 0; index < modelRendererList.size(); index++) {
            final IBone bone = modelRendererList.get(index);
            if (bone.getName().equals(name)) return bone;
        }
        return null;
    }

    @Inject(method = "clearModelRendererList", at = @At("HEAD"), require = 1)
    private void stellar_core$invalidateModelTrackers(final CallbackInfo ci) {
        stellar_core$trackerPool.invalidate();
    }
}
