package github.kasuminova.stellarcore.client.model;

import github.kasuminova.stellarcore.client.util.ClassSet;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;

import java.util.Arrays;
import java.util.stream.Stream;

public class ParallelModelLoaderAsyncBlackList extends ClassSet {
    private static final String[] BUILTIN_BLACKLIST = {
            // Immersive Engineering (0.12-98) custom loaders
            "blusunrize.immersiveengineering.client.models.obj.IEOBJLoader",
            "blusunrize.immersiveengineering.client.models.multilayer.MultiLayerLoader",
            "blusunrize.immersiveengineering.client.models.smart.ConnLoader",
            "blusunrize.immersiveengineering.client.models.smart.FeedthroughLoader",
            "blusunrize.immersiveengineering.client.models.ModelConfigurableSides$Loader",
            // Electroblob's Wizardry (4.3.14) custom loader
            "electroblob.wizardry.client.model.ModelLoaderBookshelf",
    };

    public static final ParallelModelLoaderAsyncBlackList INSTANCE = new ParallelModelLoaderAsyncBlackList();

    @Override
    public void reload() {
        classSet.clear();

        String[] userBlackList;
        try {
            userBlackList = StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoaderBlackList;
        } catch (Throwable ignored) {
            userBlackList = null;
        }

        Stream<String> userStream = userBlackList == null ? Stream.empty() : Arrays.stream(userBlackList);
        Stream<String> builtinStream = BUILTIN_BLACKLIST == null ? Stream.empty() : Arrays.stream(BUILTIN_BLACKLIST);

        Stream.concat(userStream, builtinStream)
                .filter(className -> className != null && !className.isEmpty())
                .distinct()
                .forEach(className -> findClass(className).ifPresent(this::add));
    }

}
