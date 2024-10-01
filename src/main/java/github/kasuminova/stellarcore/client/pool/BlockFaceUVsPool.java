package github.kasuminova.stellarcore.client.pool;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.pool.AsyncCanonicalizePool;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class BlockFaceUVsPool extends AsyncCanonicalizePool<float[]> {

    public static final BlockFaceUVsPool INSTANCE = new BlockFaceUVsPool();

    private final ObjectOpenCustomHashSet<float[]> pool = new ObjectOpenCustomHashSet<>(FloatArrays.HASH_STRATEGY);

    private BlockFaceUVsPool() {
    }

    @Override
    protected float[] canonicalizeInternal(final float[] target) {
        return pool.addOrGet(target);
    }

    @Override
    public void onClearPre() {
        if (StellarCoreConfig.PERFORMANCE.vanilla.blockFaceUVsCanonicalization) {
            StellarLog.LOG.info("[StellarCore-BlockFaceUVsPool] {} UVs processed. {} Unique, {} Deduplicated.",
                    getProcessedCount(), pool.size(), getProcessedCount() - pool.size()
            );
        }
    }

    @Override
    public void onClearPost() {
        if (StellarCoreConfig.PERFORMANCE.vanilla.blockFaceUVsCanonicalization) {
            StellarLog.LOG.info("[StellarCore-BlockFaceUVsPool] Pool cleared.");
        }
    }

    @Override
    protected String getName() {
        return "BlockFaceUVsPool";
    }

    @Override
    protected ObjectSet<float[]> getPoolKeySet() {
        return pool;
    }

    @Override
    protected void clearPool() {
        pool.clear();
        pool.trim();
    }

}
