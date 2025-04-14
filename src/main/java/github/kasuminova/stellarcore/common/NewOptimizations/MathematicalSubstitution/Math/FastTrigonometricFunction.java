package github.kasuminova.stellarcore.common.NewOptimizations.MathematicalSubstitution.Math;

public class FastTrigonometricFunction {
    private static final int LUT_SIZE = 1024;
    private static final int LUT_MASK = LUT_SIZE - 1;
    private static final float[] sinLUT = new float[LUT_SIZE + 1];
    private static final double TWO_PI = 6.2831853071795864769252867;
    private static final double INV_TWO_PI = 0.1591549430918953357688837;
    private static final double LUT_SCALE = LUT_SIZE * INV_TWO_PI;
    private static final int COS_OFFSET = LUT_SIZE / 4;

    static {
        for (int i = 0; i <= LUT_SIZE; i++) {
            sinLUT[i] = (float) Math.sin(i * TWO_PI / LUT_SIZE);
        }
    }

    private static double reduceAngle(double x) {
        return x - TWO_PI * Math.floor(x * INV_TWO_PI);
    }

    public static double sin(double x) {
        x = reduceAngle(x);
        double pos = x * LUT_SCALE;
        int index = (int) pos;
        index &= LUT_MASK;
        double frac = pos - index;
        return sinLUT[index] + frac * (sinLUT[(index + 1) & LUT_MASK] - sinLUT[index]);
    }

    public static double cos(double x) {
        x = reduceAngle(x);
        double pos = x * LUT_SCALE + COS_OFFSET;
        int index = (int) pos;
        index &= LUT_MASK;
        double frac = pos - index;
        return sinLUT[index] + frac * (sinLUT[(index + 1) & LUT_MASK] - sinLUT[index]);
    }

    public static double tan(double x) {
        x = reduceAngle(x);
        double pos = x * LUT_SCALE;
        int index = (int) pos;
        index &= LUT_MASK;
        double frac = pos - index;

        double sinX = sinLUT[index] + frac * (sinLUT[(index + 1) & LUT_MASK] - sinLUT[index]);

        double cosPos = pos + COS_OFFSET;
        int cosIndex = (int) cosPos;
        cosIndex &= LUT_MASK;
        double cosFrac = cosPos - cosIndex;
        double cosX = sinLUT[cosIndex] + cosFrac * (sinLUT[(cosIndex + 1) & LUT_MASK] - sinLUT[cosIndex]);

        // 处理除零情况
        if (Math.abs(cosX) < 1e-8) {
            return sinX >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        return sinX / cosX;
    }
}