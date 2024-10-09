package github.kasuminova.stellarcore.mixin.util;

public interface StellarPooledNBT {

    /**
     * 返回 Object 来兼容其他模组的反射。
     */
    Object stellar_core$getPooledNBT();

    boolean stellar_core$isPooled();

    void stellar_core$setPooled(boolean pooled);

}
