package github.kasuminova.stellarcore.client.pool;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.List;

@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
public class TLMCubesItemPool {

    private static final ObjectOpenHashSet<List<Float>> POOL = new ObjectOpenHashSet<>();
    private static long processedCount = 0;

    public static List<Float> canonicalize(final List<Float> list) {
        if (list == null) {
            return null;
        }
        FloatList floatList;
        if (list instanceof FloatList) {
            floatList = (FloatList) list;
        } else {
            floatList = new FloatArrayList(list);
        }
        synchronized (POOL) {
            processedCount++;
            return POOL.addOrGet(floatList);
        }
    }

    public static long getProcessedCount() {
        return processedCount;
    }

    public static long getUniqueCount() {
        return POOL.size();
    }

    public static void clear() {
        if (StellarCoreConfig.PERFORMANCE.tlm.modelDataCanonicalization) {
            StellarLog.LOG.info("[StellarCore-TLMCubesItemPool] {} FloatList processed. {} Unique, {} Deduplicated.",
                    processedCount, POOL.size(), processedCount - POOL.size()
            );
        }

        processedCount = 0;
        POOL.clear();
        POOL.trim();

        if (StellarCoreConfig.PERFORMANCE.tlm.modelDataCanonicalization) {
            StellarLog.LOG.info("[StellarCore-TLMCubesItemPool] Pool cleared.");
        }
    }

}
