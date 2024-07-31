package github.kasuminova.stellarcore.client.model;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public class ParallelModelLoaderAsyncBlackList {

    public static final ParallelModelLoaderAsyncBlackList INSTANCE = new ParallelModelLoaderAsyncBlackList();

    private final Set<Class<?>> blackList = new ReferenceOpenHashSet<>();

    public ParallelModelLoaderAsyncBlackList() {
        reload();
    }

    public void reload() {
        blackList.clear();
        
        Arrays.stream(StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoaderBlackList)
                .forEach(className -> findClass(className).ifPresent(this::add));
    }

    public void add(Class<?> clazz) {
        blackList.add(clazz);
    }

    public boolean isInBlackList(Class<?> clazz) {
        return blackList.contains(clazz);
    }

    private static Optional<Class<?>> findClass(String name) {
        try {
            return Optional.of(Class.forName(name));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

}
