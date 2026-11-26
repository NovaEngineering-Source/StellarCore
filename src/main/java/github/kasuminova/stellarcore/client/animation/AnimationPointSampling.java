package github.kasuminova.stellarcore.client.animation;

import software.bernie.geckolib3.core.ConstantValue;
import software.bernie.geckolib3.core.keyframe.AnimationPoint;
import software.bernie.geckolib3.core.keyframe.KeyFrame;
import software.bernie.geckolib3.core.util.Axis;
import software.bernie.shadowed.eliotlash.mclib.math.IValue;

import java.util.List;

public final class AnimationPointSampling {
    private AnimationPointSampling() {
    }

    public static AnimationPoint sample(final List<KeyFrame<IValue>> frames, final double tick,
                                        final boolean rotation, final Axis axis) {
        double total = 0;
        double frameTick = tick;
        KeyFrame<IValue> selected = null;
        for (final KeyFrame<IValue> frame : frames) {
            total += frame.getLength();
            if (total > tick) {
                selected = frame;
                frameTick = tick - (total - frame.getLength());
                break;
            }
        }
        if (selected == null) selected = frames.get(frames.size() - 1);
        double start = selected.getStartValue().get();
        double end = selected.getEndValue().get();
        if (rotation) {
            if (!(selected.getStartValue() instanceof ConstantValue)) {
                start = Math.toRadians(start);
                if (axis == Axis.X || axis == Axis.Y) start *= -1;
            }
            if (!(selected.getEndValue() instanceof ConstantValue)) {
                end = Math.toRadians(end);
                if (axis == Axis.X || axis == Axis.Y) end *= -1;
            }
        }
        return new AnimationPoint(selected, frameTick, selected.getLength(), start, end);
    }
}
