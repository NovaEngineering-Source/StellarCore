package github.kasuminova.stellarcore.common.NewOptimizations.MathematicalSubstitution;


import github.kasuminova.stellarcore.common.NewOptimizations.MathematicalSubstitution.Math.FastATrigonometricFunction;
import github.kasuminova.stellarcore.common.NewOptimizations.MathematicalSubstitution.Math.FastTrigonometricFunction;

public class CustomMath {
    public static double sin(double rad) {
        return FastTrigonometricFunction.sin(rad);
    }

    public static double cos(double rad) {
        return FastTrigonometricFunction.cos(rad);
    }

    public static double tan(double rad) {
        return FastTrigonometricFunction.tan(rad);
    }

    //public static double log(double x) {
    //    return FastLog.log(x);
    //}

    //public static double sqrt(double x) {
    //    return FastSqrt.sqrt(x);
    //}

    //public static double pow(double x, double y) {
    //    return FastPow.pow(x, y);
    //}

    //public static double exp(double x) {
    //   return FastExp.exp(x);
    //}

    public static double asin(double x) {
        return FastATrigonometricFunction.asin(x);
    }

    public static double acos(double x) {
        return FastATrigonometricFunction.acos(x);
    }

    public static double atan(double x) {
        return FastATrigonometricFunction.atan(x);
    }

    public static double atan2(double y, double x) {
        return FastATrigonometricFunction.atan2(y, x);
    }
}
