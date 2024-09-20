package github.kasuminova.stellarcore.client.pool;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.model.PositionTextureVertex;

import java.util.Objects;

@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
public class TLMPositionTextureVertexPool extends AsyncCanonicalizePool<PositionTextureVertex> {

    public static final TLMPositionTextureVertexPool INSTANCE = new TLMPositionTextureVertexPool();

    private final ObjectOpenCustomHashSet<PositionTextureVertex> pool = new ObjectOpenCustomHashSet<>(PTVStrategy.INSTANCE);

    private TLMPositionTextureVertexPool() {
    }

    @Override
    protected PositionTextureVertex canonicalizeInternal(final PositionTextureVertex target) {
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
    protected ObjectSet<PositionTextureVertex> getPoolKeySet() {
        return pool;
    }

    @Override
    protected void clearPool() {
        pool.clear();
        pool.trim();
        worker.stop();
    }

    private static class PTVStrategy implements Hash.Strategy<PositionTextureVertex> {

        private static final PTVStrategy INSTANCE = new PTVStrategy();

        @Override
        public int hashCode(final PositionTextureVertex o) {
            return Objects.hash(o.vector3D, o.texturePositionX, o.texturePositionY);
        }

        @Override
        public boolean equals(final PositionTextureVertex a, final PositionTextureVertex b) {
            if (a == b) {
                return true;
            }
            if (a == null || b == null) {
                return false;
            }
            return a.vector3D.equals(b.vector3D) && a.texturePositionX == b.texturePositionX && a.texturePositionY == b.texturePositionY;
        }

    }

}
