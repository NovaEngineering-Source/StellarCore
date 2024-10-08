package github.kasuminova.stellarcore.mixin.util;

public interface StellarNBTTagUnique {

    void stellar_core$onModified();

    void stellar_core$setUID(final long uid);

    long stellar_core$getUID();

    void stellar_core$setUnique(final boolean unique);

    /**
     * 使用 Boolean 而不是 boolean，沟槽的插件扫 Method 来获取方法。
     */
    Boolean stellar_core$isUnique();

    void stellar_core$setHashCodeCache(final int hashCode);

}
