package github.kasuminova.stellarcore.client.model;

import github.kasuminova.stellarcore.client.util.ClassSet;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;

import java.util.Arrays;

public class ParallelModelLoaderAsyncBlackList extends ClassSet {

    public static final ParallelModelLoaderAsyncBlackList INSTANCE = new ParallelModelLoaderAsyncBlackList();

    public void reload() {
        classSet.clear();
        Arrays.stream(StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoaderBlackList)
                .forEach(className -> findClass(className).ifPresent(this::add));
    }

}
