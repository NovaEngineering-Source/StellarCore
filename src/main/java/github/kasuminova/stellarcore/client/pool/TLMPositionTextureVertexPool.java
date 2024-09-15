package github.kasuminova.stellarcore.client.pool;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.client.model.PositionTextureVertex;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
public class TLMPositionTextureVertexPool {

    private static final ObjectOpenCustomHashSet<PositionTextureVertex> POOL = new ObjectOpenCustomHashSet<>(PTVStrategy.INSTANCE);
    private static final AtomicLong LOCKED = new AtomicLong(0);

    private static Future<Void> clearTask = null;
    private static long processedCount = 0;

    public static PositionTextureVertex canonicalize(final PositionTextureVertex ptv) {
        if (ptv == null) {
            return null;
        }
        LOCKED.incrementAndGet();
        synchronized (POOL) {
            processedCount++;
            PositionTextureVertex ret = POOL.addOrGet(ptv);
            LOCKED.decrementAndGet();
            return ret;
        }
    }

    public static long getProcessedCount() {
        return processedCount;
    }

    public static long getUniqueCount() {
        return POOL.size();
    }

    public static void clear() {
        if (clearTask != null) {
            if (!clearTask.isDone()) {
                return;
            }
            clearTask = null;
        }

        clearTask = CompletableFuture.runAsync(() -> {
            while (LOCKED.get() != 0) {
                try {
                    Thread.sleep(0); // waiting
                } catch (InterruptedException e) {
                    break;
                }
            }

            if (StellarCoreConfig.PERFORMANCE.tlm.texturedQuadFloatCanonicalization) {
                StellarLog.LOG.info("[StellarCore-TLMPositionTextureVertexPool] {} PositionTextureVertex processed. {} Unique, {} Deduplicated.",
                        processedCount, POOL.size(), processedCount - POOL.size()
                );
            }

            processedCount = 0;
            synchronized (POOL) {
                LOCKED.set(0);
                POOL.clear();
                POOL.trim();
            }

            if (StellarCoreConfig.PERFORMANCE.tlm.texturedQuadFloatCanonicalization) {
                StellarLog.LOG.info("[StellarCore-TLMPositionTextureVertexPool] Pool cleared.");
            }

            clearTask = null;
        });
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
