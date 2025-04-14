package github.kasuminova.stellarcore.common.NewOptimizations.MathematicalSubstitution.Math;

public class FastATrigonometricFunction {
    private static final double PI = Math.PI;
    private static final double HALF_PI = PI / 2;

    public static double atan(double x) {
        boolean neg = x < 0;
        if (neg) x = -x;
        
        boolean invert = x > 1;
        if (invert) x = 1.0 / x;

        // Pade近似式 [5/5] 在[0,1]区间
        double x2 = x * x;
        double numerator = x * (1.0 + x2 * (0.3319731356 + x2 * 0.1062067474));
        double denominator = 1.0 + x2 * (0.6312764703 + x2 * (0.230646912 + x2 * 0.010722818));
        double result = numerator / denominator;

        if (invert) result = HALF_PI - result;
        return neg ? -result : result;
    }

    public static double asin(double x) {
        if (x < -1 || x > 1) return Double.NaN;
        boolean neg = x < 0;
        if (neg) x = -x;
        
        // 对x > 0.5的情况使用恒等变换
        if (x > 0.5) {
            return HALF_PI - 2 * asin(Math.sqrt((1 - x) / 2));
        }

        // 泰勒展开到x^7项
        double x2 = x * x;
        double result = x * (1 + x2 * (1.0/6 + x2 * (3.0/40 + x2 * (5.0/112))));
        return neg ? -result : result;
    }

    public static double atan2(double y, double x) {
        if (x == 0) {
            if (y == 0) return Double.NaN;
            return y > 0 ? HALF_PI : -HALF_PI;
        }
        if (y == 0) return x > 0 ? 0 : PI;

        boolean swap = Math.abs(x) < Math.abs(y);
        double ratio = swap ? (x / y) : (y / x);
        double angle = atan(Math.abs(ratio));

        if (swap) angle = HALF_PI - angle;
        if (x < 0) angle = PI - angle;
        if (y < 0) angle = -angle;
        
        return angle;
    }

    public static double acos(double x) {
        return HALF_PI - asin(x);
    }
}