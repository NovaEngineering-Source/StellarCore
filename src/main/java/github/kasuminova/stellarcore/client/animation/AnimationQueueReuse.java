package github.kasuminova.stellarcore.client.animation;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import software.bernie.geckolib3.core.keyframe.AnimationPointQueue;
import software.bernie.geckolib3.core.keyframe.BoneAnimationQueue;
import software.bernie.geckolib3.core.processor.IBone;

import java.util.HashMap;
import java.util.List;

public final class AnimationQueueReuse {
    private final ObjectOpenHashSet<String> names = new ObjectOpenHashSet<>(2);

    public AnimationQueueReuse() {
    }

    public void reset(final HashMap<String, BoneAnimationQueue> queues, final List<IBone> bones) {
        boolean sameModel = queues.size() == bones.size();
        if (sameModel) {
            for (int index = 0; index < bones.size(); index++) {
                final IBone bone = bones.get(index);
                final BoneAnimationQueue queue = queues.get(bone.getName());
                if (queue == null || queue.bone != bone) {
                    sameModel = false;
                    break;
                }
            }
        }
        if (sameModel) {
            try {
                for (int index = 0; index < bones.size(); index++) {
                    if (!names.add(bones.get(index).getName())) {
                        sameModel = false;
                        break;
                    }
                }
            } finally {
                names.clear();
            }
        }
        if (!sameModel) {
            queues.clear();
            for (int index = 0; index < bones.size(); index++) {
                final IBone bone = bones.get(index);
                queues.put(bone.getName(), new BoneAnimationQueue(bone));
            }
            return;
        }
        for (int index = 0; index < bones.size(); index++) {
            final IBone bone = bones.get(index);
            final BoneAnimationQueue queue = queues.get(bone.getName());
            if (!ordinary(queue.rotationXQueue) || !ordinary(queue.rotationYQueue) || !ordinary(queue.rotationZQueue)
                || !ordinary(queue.positionXQueue) || !ordinary(queue.positionYQueue) || !ordinary(queue.positionZQueue)
                || !ordinary(queue.scaleXQueue) || !ordinary(queue.scaleYQueue) || !ordinary(queue.scaleZQueue)) {
                queues.put(bone.getName(), new BoneAnimationQueue(bone));
                continue;
            }
            clear(queue.rotationXQueue);
            clear(queue.rotationYQueue);
            clear(queue.rotationZQueue);
            clear(queue.positionXQueue);
            clear(queue.positionYQueue);
            clear(queue.positionZQueue);
            clear(queue.scaleXQueue);
            clear(queue.scaleYQueue);
            clear(queue.scaleZQueue);
        }
    }

    private static boolean ordinary(final AnimationPointQueue queue) {
        return queue != null && queue.getClass() == AnimationPointQueue.class;
    }

    private static void clear(final AnimationPointQueue queue) {
        queue.clear();
        queue.model = null;
    }
}
