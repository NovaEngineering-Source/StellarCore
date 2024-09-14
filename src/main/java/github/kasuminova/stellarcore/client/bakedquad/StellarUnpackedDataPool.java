package github.kasuminova.stellarcore.client.bakedquad;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import java.util.Arrays;
import java.util.Set;

@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
public class StellarUnpackedDataPool {

    private static final ObjectOpenCustomHashSet<float[][][]> POOL_LEVEL1 = new ObjectOpenCustomHashSet<>(FloatArray3DHashStrategy.INSTANCE);
    private static final ObjectOpenCustomHashSet<float[][]> POOL_LEVEL2 = new ObjectOpenCustomHashSet<>(FloatArray2DHashStrategy.INSTANCE);
    private static final ObjectOpenCustomHashSet<float[]> POOL_LEVEL3 = new ObjectOpenCustomHashSet<>(FloatArrays.HASH_STRATEGY);
    private static final ObjectOpenCustomHashSet<int[]> VERTEX_DATA_POOL = new ObjectOpenCustomHashSet<>(IntArrays.HASH_STRATEGY);

    private static long processedUnpackedCount = 0;
    private static long processedVertexDataCount = 0;

    public static float[][][] canonicalize(float[][][] data) {
        final int level = getLevel();

        if (level == 1) {
            synchronized (POOL_LEVEL1) {
                processedUnpackedCount++;
                return POOL_LEVEL1.addOrGet(data);
            }
        }
        if (level == 2) {
            synchronized (POOL_LEVEL2) {
                for (int i = 0; i < data.length; i++) {
                    processedUnpackedCount++;
                    data[i] = POOL_LEVEL2.addOrGet(data[i]);
                }
                return data;
            }
        }
        if (level == 3) {
            synchronized (POOL_LEVEL3) {
                for (int i = 0; i < data.length; i++) {
                    for (int j = 0; j < data[i].length; j++) {
                        processedUnpackedCount++;
                        data[i][j] = POOL_LEVEL3.addOrGet(data[i][j]);
                    }
                }
                return data;
            }
        }

        StellarLog.LOG.warn("[StellarCore-UnpackedDataPool] Invalid canonicalization level: {}", level);
        return data;
    }

    public static int[] canonicalize(int[] data) {
        synchronized (VERTEX_DATA_POOL) {
            processedVertexDataCount++;
            return VERTEX_DATA_POOL.addOrGet(data);
        }
    }

    private static int getLevel() {
        return StellarCoreConfig.PERFORMANCE.forge.unpackedBakedQuadDataCanonicalizationLevel;
    }

    private static Set<?> getCurrentPool() {
        int canonicalizationLevel = getLevel();
        return switch (canonicalizationLevel) {
            case 1 -> POOL_LEVEL1;
            case 2 -> POOL_LEVEL2;
            case 3 -> POOL_LEVEL3;
            default -> throw new IllegalStateException("Invalid level: " + canonicalizationLevel);
        };
    }

    public static boolean update() {
        boolean changed = false;
        int level = getLevel();
        if (level != 1 && !POOL_LEVEL1.isEmpty()) {
            POOL_LEVEL1.clear();
            POOL_LEVEL1.trim();
            StellarLog.LOG.info("[StellarCore-UnpackedDataPool] Level {} Pool cleared.", level);
            changed = true;
        }
        if (level != 2 && !POOL_LEVEL2.isEmpty()) {
            POOL_LEVEL2.clear();
            POOL_LEVEL2.trim();
            StellarLog.LOG.info("[StellarCore-UnpackedDataPool] Level {} Pool cleared.", level);
            changed = true;
        }
        if (level != 3 && !POOL_LEVEL3.isEmpty()) {
            POOL_LEVEL3.clear();
            POOL_LEVEL3.trim();
            StellarLog.LOG.info("[StellarCore-UnpackedDataPool] Level {} Pool cleared.", level);
            changed = true;
        }
        return changed;
    }

    public static void reset() {
        int canonicalizationLevel = getLevel();
        Set<?> currentPool = getCurrentPool();

        if (StellarCoreConfig.PERFORMANCE.forge.unpackedBakedQuadDataCanonicalization) {
            StellarLog.LOG.info("[StellarCore-UnpackedDataPool] {} UnpackedData processed. {} Unique, {} Deduplicated.",
                    processedUnpackedCount, currentPool.size(), processedUnpackedCount - currentPool.size()
            );
        }
        if (StellarCoreConfig.PERFORMANCE.forge.unpackedBakedQuadVertexDataCanonicalization) {
            StellarLog.LOG.info("[StellarCore-UnpackedDataPool] {} UnpackedVertexData processed. {} Unique, {} Deduplicated.",
                    processedVertexDataCount, VERTEX_DATA_POOL.size(), processedVertexDataCount - VERTEX_DATA_POOL.size()
            );
        }

        long start = System.currentTimeMillis();

        processedUnpackedCount = 0;
        processedVertexDataCount = 0;

        POOL_LEVEL1.clear();
        if (canonicalizationLevel != 1) {
            POOL_LEVEL1.trim();
        }
        POOL_LEVEL2.clear();
        if (canonicalizationLevel != 2) {
            POOL_LEVEL2.trim();
        }
        POOL_LEVEL3.clear();
        if (canonicalizationLevel != 3) {
            POOL_LEVEL3.trim();
        }

        VERTEX_DATA_POOL.clear();
        StellarLog.LOG.info("[StellarCore-UnpackedDataPool] Pool reset in {}ms.", System.currentTimeMillis() - start);
    }

    public static long getProcessedUnpackedCount() {
        return processedUnpackedCount;
    }

    public static long getProcessedVertexDataCount() {
        return processedVertexDataCount;
    }

    public static int getUnpackedUniqueCount() {
        return getCurrentPool().size();
    }

    public static int getVertexDataUniqueCount() {
        return VERTEX_DATA_POOL.size();
    }

    private static class FloatArray3DHashStrategy implements Hash.Strategy<float[][][]> {

        private static final FloatArray3DHashStrategy INSTANCE = new FloatArray3DHashStrategy();

        @Override
        public int hashCode(final float[][][] o) {
            return Arrays.deepHashCode(o);
        }

        @Override
        public boolean equals(final float[][][] a, final float[][][] b) {
            return Arrays.deepEquals(a, b);
        }

    }

    private static class FloatArray2DHashStrategy implements Hash.Strategy<float[][]> {

        private static final FloatArray2DHashStrategy INSTANCE = new FloatArray2DHashStrategy();

        @Override
        public int hashCode(final float[][] o) {
            return Arrays.deepHashCode(o);
        }

        @Override
        public boolean equals(final float[][] a, final float[][] b) {
            return Arrays.deepEquals(a, b);
        }

    }

}
