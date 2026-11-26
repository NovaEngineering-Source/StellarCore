package github.kasuminova.stellarcore.client.animation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.processor.AnimationProcessor;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.core.snapshot.DirtyTracker;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationTrackerReuseRegressionTest {
    @Test
    void cachedDistinctNamesCannotLeaveAStaleKeyAfterTheSameBoneBecomesDuplicated() throws Exception {
        final AnimationTrackerFrames.Pool pool = new AnimationTrackerFrames.Pool();
        final IBone x = bone("x");
        AnimationTrackerFrames.Frame frame = pool.borrow(2);
        frame.prepare(list(x, bone("y")));
        pool.release(frame);
        frame = pool.borrow(2);
        final List<IBone> repeated = list(x, x);
        final HashMap<String, DirtyTracker> actual = frame.prepare(repeated);
        assertMatchesOriginal(repeated, actual);
        assertEquals(1, actual.size());
        assertFalse(actual.containsKey("y"));
        pool.release(frame);

        frame = pool.borrow(2);
        final IBone last = bone("x");
        final List<IBone> sameNameDifferentBones = list(x, last);
        final HashMap<String, DirtyTracker> finalMap = frame.prepare(sameNameDifferentBones);
        assertMatchesOriginal(sameNameDifferentBones, finalMap);
        assertSame(last, finalMap.get("x").model);
        pool.release(frame);
    }

    @Test
    void reorderedNamesAndLateValidationFailureDoNotKeepPartiallyReboundOldTrackers() throws Exception {
        final AnimationTrackerFrames.Pool pool = new AnimationTrackerFrames.Pool();
        AnimationTrackerFrames.Frame frame = pool.borrow(3);
        final HashMap<String, DirtyTracker> first = frame.prepare(list(bone("x"), bone("y"), bone("z")));
        final DirtyTracker oldX = first.get("x");
        pool.release(frame);
        assertNull(oldX.model);

        frame = pool.borrow(3);
        final List<IBone> reordered = list(bone("x"), bone("z"), bone("y"));
        final HashMap<String, DirtyTracker> actual = frame.prepare(reordered);
        assertMatchesOriginal(reordered, actual);
        assertNull(oldX.model, "validate the complete order before rebinding cached models");
        pool.release(frame);

        frame = pool.borrow(3);
        final List<IBone> rotate = list(bone("y"), bone("x"), bone("z"));
        assertMatchesOriginal(rotate, frame.prepare(rotate));
        pool.release(frame);
    }

    @Test
    void repeatedPrepareWithinOneBorrowDoesNotMutateEarlierMapsOrTrackers() throws Exception {
        final AnimationTrackerFrames.Pool pool = new AnimationTrackerFrames.Pool();
        final AnimationTrackerFrames.Frame frame = pool.borrow(2);
        final IBone x = bone("x");
        final IBone y = bone("y");
        final HashMap<String, DirtyTracker> first = frame.prepare(list(x, y));
        final DirtyTracker firstX = first.get("x");
        firstX.hasScaleChanged = true;

        final List<IBone> repeated = list(x, x);
        final HashMap<String, DirtyTracker> second = frame.prepare(repeated);
        assertNotSame(first, second);
        assertNotSame(firstX, second.get("x"));
        assertMatchesOriginal(repeated, second);
        assertEquals(2, first.size());
        assertSame(x, firstX.model);
        assertSame(y, first.get("y").model);
        assertTrue(firstX.hasScaleChanged);

        final IBone replacement = bone("x");
        final List<IBone> changed = list(replacement, bone("z"));
        final HashMap<String, DirtyTracker> third = frame.prepare(changed);
        assertNotSame(first, third);
        assertNotSame(second, third);
        assertNotSame(second.get("x"), third.get("x"));
        assertMatchesOriginal(changed, third);
        assertSame(x, firstX.model);
        assertSame(x, second.get("x").model);
        pool.release(frame);
        assertNull(firstX.model, "the frame releases its own cached result");
    }

    private static void assertMatchesOriginal(final List<IBone> bones,
                                               final HashMap<String, DirtyTracker> actual) throws Exception {
        final AnimationProcessor<IAnimatable> original = new AnimationProcessor<IAnimatable>(null);
        original.getModelRendererList().addAll(bones);
        final Method create = AnimationProcessor.class.getDeclaredMethod("createNewDirtyTracker");
        create.setAccessible(true);
        @SuppressWarnings("unchecked") final HashMap<String, DirtyTracker> expected =
                (HashMap<String, DirtyTracker>) create.invoke(original);
        assertEquals(expected.keySet(), actual.keySet());
        for (final String name : expected.keySet()) {
            assertSame(expected.get(name).model, actual.get(name).model);
            assertEquals(expected.get(name).hasPositionChanged, actual.get(name).hasPositionChanged);
            assertEquals(expected.get(name).hasRotationChanged, actual.get(name).hasRotationChanged);
            assertEquals(expected.get(name).hasScaleChanged, actual.get(name).hasScaleChanged);
        }
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
}
