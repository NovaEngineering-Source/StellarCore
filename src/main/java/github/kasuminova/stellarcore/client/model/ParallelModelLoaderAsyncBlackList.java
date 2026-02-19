package github.kasuminova.stellarcore.client.model;

import github.kasuminova.stellarcore.client.util.ClassSet;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;

import java.util.Arrays;
import java.util.stream.Stream;

public class ParallelModelLoaderAsyncBlackList extends ClassSet {
    public static final ParallelModelLoaderAsyncBlackList INSTANCE = new ParallelModelLoaderAsyncBlackList();

    @Override
    public void reload() {
        classSet.clear();

        String[] userBlackList;
        String[] predefinedBlackList;
        try {
            userBlackList = StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoaderBlackList;
        } catch (Throwable ignored) {
            userBlackList = null;
        }

        try {
            predefinedBlackList = StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoaderBlackListPredefined;
        } catch (Throwable ignored) {
            predefinedBlackList = null;
        }

        Stream<String> userStream = userBlackList == null ? Stream.empty() : Arrays.stream(userBlackList);
        Stream<String> predefinedStream = predefinedBlackList == null ? Stream.empty() : Arrays.stream(predefinedBlackList);

        Stream.concat(userStream, predefinedStream)
                .filter(className -> className != null && !className.isEmpty())
                .distinct()
                .forEach(className -> findClass(className).ifPresent(this::add));
    }

}
