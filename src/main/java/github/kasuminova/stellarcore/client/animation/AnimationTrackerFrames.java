package github.kasuminova.stellarcore.client.animation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.AnimationProcessor;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.core.snapshot.DirtyTracker;
import software.bernie.shadowed.eliotlash.molang.MolangParser;

import java.util.HashMap;
import java.util.List;

/**
 * Per-processor maps are borrowed only for an exception-safe animation call, including nested calls.
 */
public final class AnimationTrackerFrames {
    private static final ThreadLocal<ObjectArrayList<Frame>> ACTIVE = ThreadLocal.withInitial(() -> new ObjectArrayList<>(2));

    private AnimationTrackerFrames() {
    }

    @SuppressWarnings({"rawtypes"})
    public static void tick(final AnimationProcessor processor, final IAnimatable entity, final Integer id,
                            final double time, final AnimationEvent event, final MolangParser parser, final boolean strict) {
        final Pool pool = ((AnimationTrackerAccess) processor).stellar_core$trackerPool();
        final Frame frame = pool.borrow(processor.getModelRendererList().size());
        frame.owner = processor;
        final ObjectArrayList<Frame> active = ACTIVE.get();
        active.add(frame);
        try {
            processor.tickAnimation(entity, id, time, event, parser, strict);
        } finally {
            active.remove(active.size() - 1);
            frame.owner = null;
            pool.release(frame);
        }
    }

    public static Frame current(final AnimationProcessor<?> processor) {
        final ObjectArrayList<Frame> active = ACTIVE.get();
        if (active.isEmpty()) return null;
        final Frame frame = active.get(active.size() - 1);
        return frame.owner == processor ? frame : null;
    }

    public static HashMap<String, DirtyTracker> fresh(final List<IBone> bones) {
        final HashMap<String, DirtyTracker> tracker = new HashMap<>();
        for (final IBone bone : bones) tracker.put(bone.getName(), new DirtyTracker(false, false, false, bone));
        return tracker;
    }

    public static final class Pool {
        private final ObjectArrayList<Frame> idle = new ObjectArrayList<>(2);
        private int generation;

        public Frame borrow(final int bones) {
            synchronized (idle) {
                return idle.isEmpty() ? new Frame(generation, bones) : idle.remove(idle.size() - 1);
            }
        }

        public void release(final Frame frame) {
            if (frame.used) {
                for (int index = 0; index < frame.values.size(); index++) frame.values.get(index).model = null;
                frame.used = false;
            }
            synchronized (idle) {
                if (frame.generation == generation && idle.size() < 2 && frame.values.size() <= 4096) idle.add(frame);
            }
        }

        public void invalidate() {
            synchronized (idle) {
                generation++;
                idle.clear();
            }
        }
    }

    public static final class Frame {
        private final int generation;
        // GeckoLib's private method and its consumers require the concrete HashMap descriptor.
        private final HashMap<String, DirtyTracker> tracker;
        private final ObjectArrayList<DirtyTracker> values = new ObjectArrayList<>();
        private AnimationProcessor<?> owner;
        private boolean used;

        private Frame(final int generation, final int bones) {
            this.generation = generation;
            tracker = new HashMap<>(Math.max(16, (int) (bones / 0.75F) + 1));
        }

        public HashMap<String, DirtyTracker> prepare(final List<IBone> bones) {
            // Only the first result belongs to this frame. Later calls retain native independent-map semantics.
            if (used) return fresh(bones);
            used = true;
            boolean reuse = tracker.size() == bones.size() && values.size() == bones.size();
            if (reuse) {
                for (int index = 0; index < bones.size(); index++) {
                    if (tracker.get(bones.get(index).getName()) != values.get(index)) {
                        reuse = false;
                        break;
                    }
                }
            }
            if (reuse) {
                for (int index = 0; index < bones.size(); index++) {
                    final DirtyTracker dirty = values.get(index);
                    dirty.model = bones.get(index);
                    dirty.hasPositionChanged = false;
                    dirty.hasRotationChanged = false;
                    dirty.hasScaleChanged = false;
                }
            } else {
                tracker.clear();
                values.clear();
                for (int index = 0; index < bones.size(); index++) {
                    final IBone bone = bones.get(index);
                    final DirtyTracker dirty = new DirtyTracker(false, false, false, bone);
                    tracker.put(bone.getName(), dirty);
                    values.add(dirty);
                }
            }
            return tracker;
        }
    }
}
