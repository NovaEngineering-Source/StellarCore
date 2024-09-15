package github.kasuminova.stellarcore.client.pool;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

public class BlockFaceUVsPool {

    private static final ObjectOpenCustomHashSet<float[]> POOL = new ObjectOpenCustomHashSet<>(FloatArrays.HASH_STRATEGY);
    private static long processedCount = 0;

    public static float[] canonicalize(final float[] uvs) {
        if (uvs == null) {
            return null;
        }
        synchronized (POOL) {
            processedCount++;
            return POOL.addOrGet(uvs);
        }
    }

    public static long getProcessedCount() {
        return processedCount;
    }

    public static int getUniqueCount() {
        return POOL.size();
    }

    public static void clear() {
        if (StellarCoreConfig.PERFORMANCE.vanilla.blockFaceUVsCanonicalization) {
            StellarLog.LOG.info("[StellarCore-BlockFaceUVsPool] {} UVs processed. {} Unique, {} Deduplicated.",
                    processedCount, POOL.size(), processedCount - POOL.size()
            );
        }

        processedCount = 0;
        POOL.clear();
        POOL.trim();

        if (StellarCoreConfig.PERFORMANCE.vanilla.blockFaceUVsCanonicalization) {
            StellarLog.LOG.info("[StellarCore-BlockFaceUVsPool] Pool cleared.");
        }
    }

}
