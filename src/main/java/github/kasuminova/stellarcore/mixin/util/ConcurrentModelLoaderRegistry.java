package github.kasuminova.stellarcore.mixin.util;

public interface ConcurrentModelLoaderRegistry {

    void stellar_core$toConcurrent();

    void stellar_core$writeToOriginalMap();

    void stellar_core$toDefault();

}
