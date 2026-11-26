package github.kasuminova.stellarcore.client.animation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib3.core.ConstantValue;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.keyframe.AnimationPoint;
import software.bernie.geckolib3.core.keyframe.AnimationPointQueue;
import software.bernie.geckolib3.core.keyframe.BoneAnimationQueue;
import software.bernie.geckolib3.core.keyframe.KeyFrame;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.core.util.Axis;
import software.bernie.shadowed.eliotlash.mclib.math.IValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationControllerAllocationReplayTest {
    private static AnimationController<IAnimatable> original() {
        return new AnimationController<IAnimatable>(null, "replay", 0F, event -> PlayState.STOP);
    }

    private static void reset(final AnimationController<?> original, final List<IBone> bones) throws Exception {
        final Method method = AnimationController.class.getDeclaredMethod("createInitialQueues", List.class);
        method.setAccessible(true);
        method.invoke(original, bones);
    }

    @Test
    void unchangedModelReusesEmptyQueuesAndClearsAllTransientModelReferences() throws Exception {
        final AnimationController<?> original = original();
        final AnimationQueueReuse reuse = new AnimationQueueReuse();
        final HashMap<String, BoneAnimationQueue> queues = new HashMap<>(); // Target's concrete ABI.
        final List<IBone> bones = list(bone("root"), bone("arm"));
        reset(original, bones);
        reuse.reset(queues, bones);
        final BoneAnimationQueue retained = queues.get("root");
        final AnimationPoint point = new AnimationPoint(null, 0D, 1D, 2D, 3D);
        for (final BoneAnimationQueue queue : queues.values()) {
            for (final AnimationPointQueue axis : axes(queue)) {
                axis.add(point);
                axis.model = queue.bone;
            }
        }
        reset(original, bones);
        reuse.reset(queues, bones);
        assertSame(retained, queues.get("root"));
        assertState(original.getBoneAnimationQueues(), queues);
    }

    @Test
    void replacingBonesUnderTheSameNamesDoesNotReuseQueuesFromTheOldModel() throws Exception {
        final AnimationController<?> original = original();
        final AnimationQueueReuse reuse = new AnimationQueueReuse();
        final HashMap<String, BoneAnimationQueue> queues = new HashMap<>();
        final List<IBone> before = list(bone("root"), bone("arm"));
        reset(original, before);
        reuse.reset(queues, before);
        final BoneAnimationQueue oldRoot = queues.get("root");
        final List<IBone> after = list(bone("root"), bone("arm"));
        reset(original, after);
        reuse.reset(queues, after);
        assertNotSame(oldRoot, queues.get("root"));
        assertState(original.getBoneAnimationQueues(), queues);
    }

    @Test
    void repeatedBoneObjectsCannotLeaveStaleKeysFromThePreviousModel() throws Exception {
        final AnimationController<?> original = original();
        final AnimationQueueReuse reuse = new AnimationQueueReuse();
        final HashMap<String, BoneAnimationQueue> queues = new HashMap<>();
        final IBone first = bone("same");
        final List<IBone> before = list(first, bone("obsolete"));
        reset(original, before);
        reuse.reset(queues, before);
        for (final List<IBone> after : listOfLists(list(first, first), list(bone("same"), bone("same")),
                list(first, first), new ObjectArrayList<IBone>())) {
            reset(original, after);
            reuse.reset(queues, after);
            assertState(original.getBoneAnimationQueues(), queues);
        }
    }

    @Test
    void customAxisQueuesAreRebuiltInsteadOfClearedAsOrdinaryQueues() throws Exception {
        final AnimationQueueReuse reuse = new AnimationQueueReuse();
        final HashMap<String, BoneAnimationQueue> queues = new HashMap<>();
        final List<IBone> bones = list(bone("root"));
        reuse.reset(queues, bones);
        final BoneAnimationQueue previous = queues.get("root");
        previous.rotationXQueue = new AnimationPointQueue() {
            @Override public void clear() { throw new AssertionError("custom queue must not be reused"); }
        };
        reuse.reset(queues, bones);
        assertNotSame(previous, queues.get("root"));
        assertTrue(queues.get("root").rotationXQueue.getClass() == AnimationPointQueue.class);
    }

    @Test
    void frameSelectionAndRotationSamplingMatchInstalledGeckoAtBoundaries() throws Exception {
        final AnimationController<?> original = original();
        final Method sample = AnimationController.class.getDeclaredMethod("getAnimationPointAtTick",
                List.class, double.class, boolean.class, Axis.class);
        sample.setAccessible(true);
        final IValue dynamicStart = () -> 180D;
        final IValue dynamicEnd = () -> -45D;
        final List<KeyFrame<IValue>> frames = new ObjectArrayList<>();
        frames.add(new KeyFrame<IValue>(0D, new ConstantValue(0.5D), dynamicEnd));
        frames.add(new KeyFrame<IValue>(5D, dynamicStart, new ConstantValue(-0.25D)));
        frames.add(new KeyFrame<IValue>(7D, dynamicStart, dynamicEnd));
        for (final double tick : new double[]{Double.NEGATIVE_INFINITY, -1D, -0D, 0D,
                Math.nextDown(5D), 5D, Math.nextUp(5D), Math.nextDown(12D), 12D, Math.nextUp(12D),
                Double.POSITIVE_INFINITY, Double.NaN}) {
            for (final boolean rotation : new boolean[]{false, true}) {
                for (final Axis axis : new Axis[]{Axis.X, Axis.Y, Axis.Z, null}) {
                    final AnimationPoint expected = (AnimationPoint) sample.invoke(original, frames, tick, rotation, axis);
                    final AnimationPoint actual = AnimationPointSampling.sample(frames, tick, rotation, axis);
                    assertSame(expected.keyframe, actual.keyframe);
                    assertEquals(expected.currentTick, actual.currentTick);
                    assertEquals(expected.animationEndTick, actual.animationEndTick);
                    assertEquals(expected.animationStartValue, actual.animationStartValue);
                    assertEquals(expected.animationEndValue, actual.animationEndValue);
                }
            }
        }
        final List<KeyFrame<IValue>> empty = new ObjectArrayList<>();
        final InvocationTargetException expected = assertThrows(InvocationTargetException.class,
                () -> sample.invoke(original, empty, 0D, false, Axis.X));
        final RuntimeException actual = assertThrows(RuntimeException.class,
                () -> AnimationPointSampling.sample(empty, 0D, false, Axis.X));
        assertEquals(expected.getCause().getClass(), actual.getClass());
    }

    private static void assertState(final HashMap<String, BoneAnimationQueue> expected,
                                    final HashMap<String, BoneAnimationQueue> actual) {
        assertEquals(expected.keySet(), actual.keySet());
        for (final String name : expected.keySet()) {
            assertSame(expected.get(name).bone, actual.get(name).bone);
            final AnimationPointQueue[] expectedAxes = axes(expected.get(name));
            final AnimationPointQueue[] actualAxes = axes(actual.get(name));
            for (int i = 0; i < actualAxes.length; i++) {
                assertEquals(expectedAxes[i], actualAxes[i]);
                assertNull(actualAxes[i].model);
            }
        }
    }

    private static AnimationPointQueue[] axes(final BoneAnimationQueue queue) {
        return new AnimationPointQueue[]{queue.rotationXQueue, queue.rotationYQueue, queue.rotationZQueue,
                queue.positionXQueue, queue.positionYQueue, queue.positionZQueue,
                queue.scaleXQueue, queue.scaleYQueue, queue.scaleZQueue};
    }

    static IBone bone(final String name) {
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

    @SafeVarargs
    private static List<List<IBone>> listOfLists(final List<IBone>... groups) {
        final List<List<IBone>> result = new ObjectArrayList<>();
        for (final List<IBone> group : groups) result.add(group);
        return result;
    }
}
