package github.kasuminova.stellarcore.client.pool;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.pool.AsyncCanonicalizePool;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.List;

@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
public class TLMCubesItemPool extends AsyncCanonicalizePool<List<Float>> {

    public static final TLMCubesItemPool INSTANCE = new TLMCubesItemPool();

    private final ObjectOpenHashSet<List<Float>> pool = new ObjectOpenHashSet<>();

    private TLMCubesItemPool() {
    }

    @Override
    protected List<Float> preProcess(final List<Float> list) {
        return list instanceof FloatList ? list : new FloatArrayList(list);
    }

    @Override
    protected List<Float> canonicalizeInternal(final List<Float> target) {
        return pool.addOrGet(target);
    }

    @Override
    public void onClearPre() {
        if (StellarCoreConfig.PERFORMANCE.tlm.texturedQuadFloatCanonicalization) {
            StellarLog.LOG.info("[StellarCore-TLMPositionTextureVertexPool] {} PositionTextureVertex processed. {} Unique, {} Deduplicated.",
                    getProcessedCount(), pool.size(), getProcessedCount() - pool.size()
            );
        }
    }

    @Override
    public void onClearPost() {
        if (StellarCoreConfig.PERFORMANCE.tlm.texturedQuadFloatCanonicalization) {
            StellarLog.LOG.info("[StellarCore-TLMPositionTextureVertexPool] Pool cleared.");
        }
    }

    @Override
    protected String getName() {
        return "TLMPositionTextureVertexPool";
    }

    @Override
    protected ObjectSet<List<Float>> getPoolKeySet() {
        return pool;
    }

    @Override
    protected void clearPool() {
        pool.clear();
        pool.trim();
        worker.stop();
    }

}
