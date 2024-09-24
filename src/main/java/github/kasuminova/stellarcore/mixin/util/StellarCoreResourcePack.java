package github.kasuminova.stellarcore.mixin.util;

public interface StellarCoreResourcePack {

    void stellar_core$onReload();

    void stellar_core$disableCache();

    void stellar_core$enableCache();

    default boolean stellar_core$isPersistent() {
        return false;
    }

}
