package github.kasuminova.stellarcore.client.animation;

import github.kasuminova.stellarcore.mixin.geckolib.MixinAnimationProcessorTrackers;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.AnimationProcessor;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.core.snapshot.DirtyTracker;
import software.bernie.shadowed.eliotlash.molang.MolangParser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnimationTrackerFramesTest {
    @Test
    void preparationMatchesInstalledDirtyTrackerCreationAndClearsModelsOnRelease() throws Exception {
        final AnimationProcessor<IAnimatable> original = new AnimationProcessor<IAnimatable>(null);
        final Method create = AnimationProcessor.class.getDeclaredMethod("createNewDirtyTracker");
        create.setAccessible(true);
        final AnimationTrackerFrames.Pool pool = new AnimationTrackerFrames.Pool();
        final AnimationTrackerFrames.Frame frame = pool.borrow(2);
        final List<IBone> bones = list(bone("root"), bone("arm"));
        original.getModelRendererList().addAll(bones);
        @SuppressWarnings("unchecked") final HashMap<String, DirtyTracker> expected =
                (HashMap<String, DirtyTracker>) create.invoke(original);
        final HashMap<String, DirtyTracker> actual = frame.prepare(bones);
        assertTrackers(expected, actual);
        final DirtyTracker retained = actual.get("root");
        retained.hasPositionChanged = true;
        retained.hasRotationChanged = true;
        retained.hasScaleChanged = true;
        pool.release(frame);
        assertModelsReleased(frame);
        final AnimationTrackerFrames.Frame reused = pool.borrow(bones.size());
        assertSame(frame, reused);
        assertSame(retained, reused.prepare(bones).get("root"));
        assertFalse(retained.hasPositionChanged);
        assertFalse(retained.hasRotationChanged);
        assertFalse(retained.hasScaleChanged);
        pool.release(reused);
    }

    @Test
    void changedBonesAndDuplicateNamesKeepTheOriginalLastValueAndReleaseEveryTracker() throws Exception {
        final AnimationTrackerFrames.Pool pool = new AnimationTrackerFrames.Pool();
        final AnimationTrackerFrames.Frame frame = pool.borrow(2);
        frame.prepare(list(bone("old-root"), bone("old-arm")));
        final IBone first = bone("same");
        final IBone last = bone("same");
        final List<IBone> duplicates = list(first, last);
        final AnimationProcessor<IAnimatable> original = new AnimationProcessor<IAnimatable>(null);
        original.getModelRendererList().addAll(duplicates);
        final Method create = AnimationProcessor.class.getDeclaredMethod("createNewDirtyTracker");
        create.setAccessible(true);
        @SuppressWarnings("unchecked") final HashMap<String, DirtyTracker> expected =
                (HashMap<String, DirtyTracker>) create.invoke(original);
        final HashMap<String, DirtyTracker> actual = frame.prepare(duplicates);
        assertTrackers(expected, actual);
        assertSame(last, actual.get("same").model);
        pool.release(frame);
        assertModelsReleased(frame);
    }

    @Test
    void sameProcessorReentryUsesIndependentMapsAndRestoresTheOuterFrame() throws Exception {
        final Fixture processor = new Fixture();
        processor.getModelRendererList().add(bone("root"));
        final List<AnimationTrackerFrames.Frame> frames = new ObjectArrayList<>();
        final List<HashMap<String, DirtyTracker>> maps = new ObjectArrayList<>();
        final int[] depth = {0};
        processor.body = () -> {
            final AnimationTrackerFrames.Frame current = AnimationTrackerFrames.current(processor);
            frames.add(current);
            final HashMap<String, DirtyTracker> map = current.prepare(processor.getModelRendererList());
            maps.add(map);
            if (depth[0]++ == 0) {
                try {
                    tick(processor);
                    assertSame(current, AnimationTrackerFrames.current(processor));
                    assertSame(processor.getModelRendererList().get(0), map.get("root").model);
                    assertNull(maps.get(1).get("root").model);
                } finally {
                    depth[0]--;
                }
            } else {
                depth[0]--;
            }
        };
        tick(processor);
        assertNotSame(frames.get(0), frames.get(1));
        assertNotSame(maps.get(0), maps.get(1));
        assertNull(AnimationTrackerFrames.current(processor));
        assertModelsReleased(frames.get(0));
        assertModelsReleased(frames.get(1));
    }

    @Test
    void otherProcessorReentryDoesNotExposeTheOuterFrameToTheInnerProcessor() throws Exception {
        final Fixture outer = new Fixture();
        final Fixture inner = new Fixture();
        outer.getModelRendererList().add(bone("outer"));
        inner.getModelRendererList().add(bone("inner"));
        final List<AnimationTrackerFrames.Frame> frames = new ObjectArrayList<>();
        inner.body = () -> {
            assertNull(AnimationTrackerFrames.current(outer));
            final AnimationTrackerFrames.Frame frame = AnimationTrackerFrames.current(inner);
            frames.add(frame);
            frame.prepare(inner.getModelRendererList());
        };
        outer.body = () -> {
            final AnimationTrackerFrames.Frame frame = AnimationTrackerFrames.current(outer);
            frames.add(frame);
            frame.prepare(outer.getModelRendererList());
            tick(inner);
            assertSame(frame, AnimationTrackerFrames.current(outer));
        };
        tick(outer);
        assertNotSame(frames.get(0), frames.get(1));
        assertNull(AnimationTrackerFrames.current(outer));
        assertNull(AnimationTrackerFrames.current(inner));
        assertModelsReleased(frames.get(0));
        assertModelsReleased(frames.get(1));
    }

    @Test
    void exceptionsReleaseOwnerModelsAndStackBeforeTheFrameIsBorrowedAgain() throws Exception {
        final Fixture processor = new Fixture();
        processor.getModelRendererList().add(bone("root"));
        final AnimationTrackerFrames.Frame[] frame = new AnimationTrackerFrames.Frame[1];
        final IllegalStateException failure = new IllegalStateException("animation callback failure");
        processor.body = () -> {
            frame[0] = AnimationTrackerFrames.current(processor);
            frame[0].prepare(processor.getModelRendererList());
            throw failure;
        };
        assertSame(failure, assertThrows(IllegalStateException.class, () -> tick(processor)));
        assertNull(AnimationTrackerFrames.current(processor));
        assertModelsReleased(frame[0]);
        final Field owner = AnimationTrackerFrames.Frame.class.getDeclaredField("owner");
        owner.setAccessible(true);
        assertNull(owner.get(frame[0]));
        final AnimationTrackerFrames.Frame next = processor.pool.borrow(1);
        assertSame(frame[0], next);
        processor.pool.release(next);
    }

    @Test
    void nestedExceptionsUnwindBothProcessorsAndTheirBorrowedModels() throws Exception {
        final Fixture outer = new Fixture();
        final Fixture inner = new Fixture();
        outer.getModelRendererList().add(bone("outer"));
        inner.getModelRendererList().add(bone("inner"));
        final List<AnimationTrackerFrames.Frame> frames = new ObjectArrayList<>();
        final IllegalArgumentException failure = new IllegalArgumentException("nested animation failure");
        inner.body = () -> {
            final AnimationTrackerFrames.Frame frame = AnimationTrackerFrames.current(inner);
            frames.add(frame);
            frame.prepare(inner.getModelRendererList());
            throw failure;
        };
        outer.body = () -> {
            final AnimationTrackerFrames.Frame frame = AnimationTrackerFrames.current(outer);
            frames.add(frame);
            frame.prepare(outer.getModelRendererList());
            tick(inner);
        };
        assertSame(failure, assertThrows(IllegalArgumentException.class, () -> tick(outer)));
        assertNull(AnimationTrackerFrames.current(outer));
        assertNull(AnimationTrackerFrames.current(inner));
        assertModelsReleased(frames.get(0));
        assertModelsReleased(frames.get(1));
    }

    @Test
    void invalidationDuringAnActiveCallDiscardsItsFrameAfterRelease() throws Exception {
        final Fixture processor = new Fixture();
        processor.getModelRendererList().add(bone("root"));
        final AnimationTrackerFrames.Frame[] active = new AnimationTrackerFrames.Frame[1];
        processor.body = () -> {
            active[0] = AnimationTrackerFrames.current(processor);
            active[0].prepare(processor.getModelRendererList());
            processor.pool.invalidate();
        };
        tick(processor);
        assertModelsReleased(active[0]);
        final AnimationTrackerFrames.Frame next = processor.pool.borrow(1);
        assertNotSame(active[0], next);
        processor.pool.release(next);
    }

    @Test
    void freshFallbackCallsDoNotShareTheirMapsOrTrackers() {
        final List<IBone> bones = list(bone("root"));
        final HashMap<String, DirtyTracker> first = AnimationTrackerFrames.fresh(bones);
        final HashMap<String, DirtyTracker> second = AnimationTrackerFrames.fresh(bones);
        assertNotSame(first, second);
        assertNotSame(first.get("root"), second.get("root"));
    }

    @Test
    void productionGetBoneKeepsFirstDuplicateMatchAndPrimitiveIdsKeepNullMembership() throws Exception {
        final AnimationProcessor<IAnimatable> original = new AnimationProcessor<IAnimatable>(null);
        final MixinAnimationProcessorTrackers candidate = new MixinAnimationProcessorTrackers() { };
        final List<IBone> bones = list(bone("same"), bone("same"), bone("arm"));
        original.getModelRendererList().addAll(bones);
        final Field list = MixinAnimationProcessorTrackers.class.getDeclaredField("modelRendererList");
        list.setAccessible(true);
        list.set(candidate, bones);
        for (final String name : new String[]{"same", "arm", "missing", null}) {
            assertSame(original.getBone(name), candidate.getBone(name));
        }
        final Method clear = MixinAnimationProcessorTrackers.class.getDeclaredMethod("stellar_core$clearAnimatedIds", Set.class);
        final Method add = MixinAnimationProcessorTrackers.class.getDeclaredMethod("stellar_core$addAnimatedId", Set.class, Object.class);
        final Method contains = MixinAnimationProcessorTrackers.class.getDeclaredMethod("stellar_core$containsAnimatedId", Set.class, Object.class);
        clear.setAccessible(true);
        add.setAccessible(true);
        contains.setAccessible(true);
        final Set<Integer> expected = new ObjectOpenHashSet<>();
        for (int round = 0; round < 2; round++) {
            expected.clear();
            clear.invoke(candidate, expected);
            for (final Integer value : new Integer[]{null, 0, 1024, 1024, Integer.MIN_VALUE, Integer.MAX_VALUE, null}) {
                assertEquals(expected.contains(value), contains.invoke(candidate, expected, value));
                assertEquals(expected.add(value), add.invoke(candidate, expected, value));
                assertEquals(expected.contains(value), contains.invoke(candidate, expected, value));
            }
        }
    }

    private static void assertTrackers(final HashMap<String, DirtyTracker> expected,
                                       final HashMap<String, DirtyTracker> actual) {
        assertEquals(expected.keySet(), actual.keySet());
        for (final String name : expected.keySet()) {
            assertSame(expected.get(name).model, actual.get(name).model);
            assertEquals(expected.get(name).hasPositionChanged, actual.get(name).hasPositionChanged);
            assertEquals(expected.get(name).hasRotationChanged, actual.get(name).hasRotationChanged);
            assertEquals(expected.get(name).hasScaleChanged, actual.get(name).hasScaleChanged);
        }
    }

    private static void assertModelsReleased(final AnimationTrackerFrames.Frame frame) throws Exception {
        final Field values = AnimationTrackerFrames.Frame.class.getDeclaredField("values");
        values.setAccessible(true);
        for (final Object value : (List<?>) values.get(frame)) assertNull(((DirtyTracker) value).model);
    }

    private static void tick(final Fixture processor) {
        AnimationTrackerFrames.tick(processor, null, null, 0D, null, null, false);
    }

    private static IBone bone(final String name) {
        return (IBone) Proxy.newProxyInstance(IBone.class.getClassLoader(), new Class<?>[]{IBone.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return name;
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("toString")) return "bone:" + name;
                    if (method.getReturnType() == float.class) return 0F;
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
    }

    private static List<IBone> list(final IBone... bones) {
        final List<IBone> result = new ObjectArrayList<>();
        for (final IBone bone : bones) result.add(bone);
        return result;
    }

    private static final class Fixture extends AnimationProcessor<IAnimatable> implements AnimationTrackerAccess {
        private final AnimationTrackerFrames.Pool pool = new AnimationTrackerFrames.Pool();
        private Runnable body;

        private Fixture() { super(null); }

        @Override
        public AnimationTrackerFrames.Pool stellar_core$trackerPool() { return pool; }

        @Override
        @SuppressWarnings("rawtypes")
        public void tickAnimation(final IAnimatable entity, final Integer uniqueID, final double seekTime,
                                  final AnimationEvent event, final MolangParser parser, final boolean strict) {
            body.run();
        }
    }
}
