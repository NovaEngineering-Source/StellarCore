package github.kasuminova.stellarcore.common.util;

import github.kasuminova.stellarcore.mixin.StellarCoreEarlyMixinLoader;

public class StellarEnvironment {

    private static int processors = 0;

    /**
     * 在 {@link StellarCoreEarlyMixinLoader} 处静态加载处调用，主要防止线程亲和导致始终返回 1 的问题。
     */
    public static void init() {
        processors = Runtime.getRuntime().availableProcessors();
        StellarLog.LOG.info("[StellarEnvironment] Processor count: {}", processors);
        if (!shouldParallel()) {
            StellarLog.LOG.warn("[StellarEnvironment] Processor count is less than 3, parallel processing is disabled.");
        } else {
            StellarLog.LOG.info("[StellarEnvironment] Parallel execution is enabled.");
        }
    }

    public static boolean shouldParallel() {
        return processors > 2;
    }

    public static int getConcurrency() {
        return shouldParallel() ? processors : 1;
    }

}
