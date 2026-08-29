package github.kasuminova.stellarcore.mixin.util;

public interface StellarCoreResourcePack {

    void stellar_core$onReload();

    void stellar_core$disableCache();

    void stellar_core$enableCache();

    /**
     * Returns whether this resource pack can gain or lose resource domains while the game is
     * running.  The resource manager uses this marker to refresh only mutable directory packs
     * after a resource lookup misses.
     */
    default boolean stellar_core$isMutableResourcePack() {
        return false;
    }

    default boolean stellar_core$isPersistent() {
        return false;
    }

}
