package github.kasuminova.stellarcore.client.model;

import github.kasuminova.stellarcore.client.util.ClassBlackList;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;

import java.util.Arrays;

public class ParallelModelLoaderAsyncBlackList extends ClassBlackList {

    public static final ParallelModelLoaderAsyncBlackList INSTANCE = new ParallelModelLoaderAsyncBlackList();

    public void reload() {
        blackList.clear();
        Arrays.stream(StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoaderBlackList)
                .forEach(className -> findClass(className).ifPresent(this::add));
    }

}
