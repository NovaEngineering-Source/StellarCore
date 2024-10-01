package github.kasuminova.stellarcore.client.pool;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.pool.AsyncCanonicalizePool;
import github.kasuminova.stellarcore.common.pool.CanonicalizeWorker;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
public class StellarUnpackedDataPool {

    private static final UnpackedDataPoolLevel1 POOL_LEVEL1 = new UnpackedDataPoolLevel1();
    private static final UnpackedDataPoolLevel2 POOL_LEVEL2 = new UnpackedDataPoolLevel2();
    private static final UnpackedDataPoolLevel3 POOL_LEVEL3 = new UnpackedDataPoolLevel3();

    private static final VertexDataPool VERTEX_POOL = new VertexDataPool();

    static {
        update();
    }

    @SuppressWarnings("ArrayEquality")
    public static void canonicalizeAsync(final float[][][] unpackedData, final Consumer<float[][][]> callback) {
        POOL_LEVEL1.canonicalizeAsync(unpackedData, floatsL1 -> {
            final int level = getLevel();
            // Level 2
            if (unpackedData != floatsL1 && level >= 2) {
                for (int i = 0; i < floatsL1.length; i++) {
                    float[][] floatsL2 = POOL_LEVEL2.canonicalize(floatsL1[i]);
                    if (floatsL1[i] == floatsL2) {
                        continue;
                    }

                    floatsL1[i] = floatsL2;
                    // Level 3
                    if (level >= 3) {
                        for (int j = 0; j < floatsL1[i].length; j++) {
                            floatsL1[i][j] = POOL_LEVEL3.canonicalize(floatsL1[i][j]);
                        }
                    }
                }
            }
            callback.accept(floatsL1);
        });
    }

    public static float[][][] canonicalize(float[][][] data) {
        final int level = getLevel();

        if (level == 1) {
            return POOL_LEVEL1.canonicalize(data);
        }
        if (level == 2) {
            for (int i = 0; i < data.length; i++) {
                data[i] = POOL_LEVEL2.canonicalize(data[i]);
            }
            return data;
        }
        if (level == 3) {
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {
                    data[i][j] = POOL_LEVEL3.canonicalize(data[i][j]);
                }
            }
            return data;
        }

        StellarLog.LOG.warn("[StellarCore-UnpackedDataPool] Invalid canonicalization level: {}", level);
        return data;
    }

    public static void canonicalizeAsync(final int[] data, final Consumer<int[]> callback) {
        VERTEX_POOL.canonicalizeAsync(data, callback);
    }

    public static int[] canonicalize(int[] data) {
        return VERTEX_POOL.canonicalize(data);
    }

    private static int getLevel() {
        return StellarCoreConfig.PERFORMANCE.forge.unpackedBakedQuadDataCanonicalizationLevel;
    }

    private static List<UnpackedDataPool<?>> getCurrentPools() {
        int canonicalizationLevel = getLevel();
        return switch (canonicalizationLevel) {
            case 1 -> Collections.singletonList(POOL_LEVEL1);
            case 2 -> Arrays.asList(POOL_LEVEL1, POOL_LEVEL2);
            case 3 -> Arrays.asList(POOL_LEVEL1, POOL_LEVEL2, POOL_LEVEL3);
            default -> throw new IllegalStateException("Invalid canonicalization level: " + canonicalizationLevel);
        };
    }

    public static boolean update() {
        boolean changed = false;
        int level = getLevel();
//        if (level != 1 && !POOL_LEVEL1.getPoolKeySet().isEmpty()) {
//            POOL_LEVEL1.clearAndTrim();
//            StellarLog.LOG.info("[StellarCore-UnpackedDataPool] Level {} Pool cleared.", level);
//            changed = true;
//        }
        if (level < 2 && !POOL_LEVEL2.getPoolKeySet().isEmpty()) {
            POOL_LEVEL2.clearAndTrim();
            StellarLog.LOG.info("[StellarCore-UnpackedDataPool] Level {} Pool cleared.", level);
            changed = true;
        }
        if (level < 3 && !POOL_LEVEL3.getPoolKeySet().isEmpty()) {
            POOL_LEVEL3.clearAndTrim();
            StellarLog.LOG.info("[StellarCore-UnpackedDataPool] Level {} Pool cleared.", level);
            changed = true;
        }
        for (final UnpackedDataPool<?> currentPool : getCurrentPools()) {
            CanonicalizeWorker<?> worker = currentPool.getWorker();
            if (!worker.isRunning()) {
                worker.start();
            }
        }
        return changed;
    }

    public static void reset() {
        int canonicalizationLevel = getLevel();
        long currentPoolProcessedCount = getProcessedUnpackedDataCount();
        long uniqueCount = getUnpackedDataUniqueCount();

        if (StellarCoreConfig.PERFORMANCE.forge.unpackedBakedQuadDataCanonicalization) {
            StellarLog.LOG.info("[StellarCore-UnpackedDataPool] {} UnpackedData processed. {} Unique, {} Deduplicated.",
                    currentPoolProcessedCount, uniqueCount, currentPoolProcessedCount - uniqueCount
            );
        }
        if (StellarCoreConfig.PERFORMANCE.forge.unpackedBakedQuadVertexDataCanonicalization || StellarCoreConfig.PERFORMANCE.vanilla.bakedQuadVertexDataCanonicalization) {
            StellarLog.LOG.info("[StellarCore-VertexDataPool] {} VertexData processed. {} Unique, {} Deduplicated.",
                    VERTEX_POOL.getProcessedCount(), VERTEX_POOL.getUniqueCount(), VERTEX_POOL.getProcessedCount() - VERTEX_POOL.getUniqueCount()
            );
        }

        long start = System.currentTimeMillis();

        if (canonicalizationLevel >= 1) {
            POOL_LEVEL1.clear();
        } else {
            POOL_LEVEL1.clearAndTrim();
        }
        if (canonicalizationLevel >= 2) {
            POOL_LEVEL2.clear();
        } else {
            POOL_LEVEL2.clearAndTrim();
        }
        if (canonicalizationLevel >= 3) {
            POOL_LEVEL3.clear();
        } else {
            POOL_LEVEL3.clearAndTrim();
        }

        VERTEX_POOL.clear();
        StellarLog.LOG.info("[StellarCore-UnpackedDataPool] Pool reset in {}ms.", System.currentTimeMillis() - start);
    }

    public static long getProcessedUnpackedDataCount() {
        return getCurrentPools().stream().mapToLong(UnpackedDataPool::getProcessedCount).sum();
    }

    public static int getUnpackedDataUniqueCount() {
        return getCurrentPools().stream().mapToInt(UnpackedDataPool::getUniqueCount).sum();
    }

    public static long getProcessedVertexDataCount() {
        return VERTEX_POOL.getProcessedCount();
    }

    public static int getVertexDataUniqueCount() {
        return VERTEX_POOL.getPoolKeySet().size();
    }

    public abstract static class UnpackedDataPool<T> extends AsyncCanonicalizePool<T> {

        private final ObjectOpenCustomHashSet<T> pool = new ObjectOpenCustomHashSet<>(getStrategy());

        private boolean trim = false;

        protected abstract Hash.Strategy<T> getStrategy();

        @Override
        protected T canonicalizeInternal(final T target) {
            return pool.addOrGet(target);
        }

        @Override
        protected ObjectSet<T> getPoolKeySet() {
            return pool;
        }

        @Override
        protected void clearPool() {
            pool.clear();
            if (trim) {
                pool.trim();
                trim = false;
                getWorker().stop();
            }
        }

        protected void clearAndTrim() {
            trim = true;
            clear();
        }

    }

    public static class UnpackedDataPoolLevel1 extends UnpackedDataPool<float[][][]> {

        @Override
        protected Hash.Strategy<float[][][]> getStrategy() {
            return FloatArray3DHashStrategy.INSTANCE;
        }

        @Override
        protected String getName() {
            return "UnpackedDataPool-L1";
        }

    }

    public static class UnpackedDataPoolLevel2 extends UnpackedDataPool<float[][]> {

        @Override
        protected Hash.Strategy<float[][]> getStrategy() {
            return FloatArray2DHashStrategy.INSTANCE;
        }

        @Override
        protected String getName() {
            return "UnpackedDataPool-L2";
        }

    }

    public static class UnpackedDataPoolLevel3 extends UnpackedDataPool<float[]> {

        @Override
        protected Hash.Strategy<float[]> getStrategy() {
            return FloatArrays.HASH_STRATEGY;
        }

        @Override
        protected String getName() {
            return "UnpackedDataPool-L3";
        }

    }

    public static class VertexDataPool extends AsyncCanonicalizePool<int[]> {

        private final ObjectOpenCustomHashSet<int[]> pool = new ObjectOpenCustomHashSet<>(IntArrays.HASH_STRATEGY);

        @Override
        protected int[] canonicalizeInternal(final int[] target) {
            return pool.addOrGet(target);
        }

        @Override
        protected String getName() {
            return "VertexDataPool";
        }

        @Override
        protected ObjectSet<int[]> getPoolKeySet() {
            return pool;
        }

        @Override
        protected void clearPool() {
            pool.clear();
        }

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
