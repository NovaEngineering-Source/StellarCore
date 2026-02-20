package github.kasuminova.stellarcore.jmh;

import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingIdentityHashMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.ThreadParams;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Map micro-benchmark for StellarCore's ParallelModelLoader-related workloads.
 *
 * Workload model / 负载模型：
 * - read-only: get(hit)
 * - write-only: put(update existing)
 * - mixed: get(hit) + put(update existing)
 * - keys are per-thread sharded to reduce unrealistic hot-key contention
 * - writes alternate between two pre-allocated values to force real updates
 *
 * Key modes / Key 形态：
 * - IDENTITY: keys are plain Objects (models often behave like identity keys)
 * - RESOURCE_LIKE: keys mimic ResourceLocation-style equals/hash (canonical instances)
 *
 * Note: NonBlockingIdentityHashMap is only semantically valid when keys are canonicalized
 * (i.e., the same instance is used for lookups). This benchmark keeps keys canonical.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ParallelModelLoaderMapJmhBenchmark {

    public enum MapImpl {
        CHM,
        NBHM,
        NBIHM
    }

    public enum KeyMode {
        IDENTITY,
        RESOURCE_LIKE
    }

    @State(Scope.Benchmark)
    public static class MapState {

        @Param({"CHM", "NBHM", "NBIHM"})
        public MapImpl mapImpl;

        @Param({"IDENTITY", "RESOURCE_LIKE"})
        public KeyMode keyMode;

        /** Must be >= threadCount, large enough to reduce contention. */
        @Param({"16384", "65536"})
        public int keyCount;

        public Object[] keys;
        public Object[] valuesA;
        public Object[] valuesB;
        public ConcurrentMap<Object, Object> map;

        @Setup(Level.Iteration)
        public void setupIteration() {
            if (keyCount <= 0) {
                throw new IllegalArgumentException("keyCount must be > 0");
            }

            keys = new Object[keyCount];
            valuesA = new Object[keyCount];
            valuesB = new Object[keyCount];

            switch (keyMode) {
                case IDENTITY:
                    for (int i = 0; i < keyCount; i++) {
                        keys[i] = new Object();
                    }
                    break;
                case RESOURCE_LIKE:
                    for (int i = 0; i < keyCount; i++) {
                        keys[i] = new ResourceKey(i);
                    }
                    break;
                default:
                    throw new AssertionError(keyMode);
            }

            for (int i = 0; i < keyCount; i++) {
                // Pre-allocate values to avoid measuring allocation/GC.
                // We alternate between A/B to ensure put() performs a real update.
                valuesA[i] = new Object();
                valuesB[i] = new Object();
            }

            map = createMap(mapImpl, keyCount);
            for (int i = 0; i < keyCount; i++) {
                map.put(keys[i], valuesA[i]);
            }
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static ConcurrentMap<Object, Object> createMap(final MapImpl impl, final int expectedSize) {
            final int initialCapacity = (int) (expectedSize / 0.75f) + 1;
            switch (impl) {
                case CHM:
                    return new ConcurrentHashMap<>(initialCapacity);
                case NBHM:
                    return (ConcurrentMap) new NonBlockingHashMap(expectedSize);
                case NBIHM:
                    return (ConcurrentMap) new NonBlockingIdentityHashMap(expectedSize);
                default:
                    throw new AssertionError(impl);
            }
        }
    }

    @State(Scope.Thread)
    public static class ThreadState {

        private int cursor;
        private int writeToggle;
        private int sliceOffset;
        private int sliceSize;
        private int sliceMask;

        @Setup(Level.Iteration)
        public void setupIteration(final MapState mapState, final ThreadParams params) {
            cursor = 0;
            writeToggle = 0;

            final int threads = Math.max(1, params.getThreadCount());
            int perThread = mapState.keyCount / threads;
            if (perThread <= 0) {
                perThread = 1;
            }

            sliceSize = perThread;
            sliceOffset = perThread * params.getThreadIndex();

            // use bitmask when possible, otherwise fall back to modulo
            sliceMask = (Integer.bitCount(sliceSize) == 1) ? (sliceSize - 1) : -1;

            // Defensive clamp: in weird configurations (threads > keyCount) sliceOffset can overflow keyCount.
            if (sliceOffset >= mapState.keyCount) {
                sliceOffset = sliceOffset % mapState.keyCount;
            }
        }

        public int nextIndex(final MapState mapState) {
            final int i = cursor++;
            final int inSlice = (sliceMask != -1) ? (i & sliceMask) : (i % sliceSize);
            final int idx = sliceOffset + inSlice;
            return (idx < mapState.keyCount) ? idx : (idx % mapState.keyCount);
        }

        public Object nextWriteValue(final MapState mapState, final int idx) {
            writeToggle ^= 1;
            return writeToggle == 0 ? mapState.valuesA[idx] : mapState.valuesB[idx];
        }
    }

    @Benchmark
    @Threads(1)
    public void singleThread_get_hit(final MapState state, final ThreadState thread, final Blackhole bh) {
        final int idx = thread.nextIndex(state);
        final Object key = state.keys[idx];

        bh.consume(state.map.get(key));
    }

    @Benchmark
    @Threads(Threads.MAX)
    public void concurrent_get_hit(final MapState state, final ThreadState thread, final Blackhole bh) {
        final int idx = thread.nextIndex(state);
        final Object key = state.keys[idx];

        bh.consume(state.map.get(key));
    }

    @Benchmark
    @Threads(1)
    public void singleThread_put_updateExisting(final MapState state, final ThreadState thread, final Blackhole bh) {
        final int idx = thread.nextIndex(state);
        final Object key = state.keys[idx];
        final Object value = thread.nextWriteValue(state, idx);

        bh.consume(state.map.put(key, value));
    }

    @Benchmark
    @Threads(Threads.MAX)
    public void concurrent_put_updateExisting(final MapState state, final ThreadState thread, final Blackhole bh) {
        final int idx = thread.nextIndex(state);
        final Object key = state.keys[idx];
        final Object value = thread.nextWriteValue(state, idx);

        bh.consume(state.map.put(key, value));
    }

    @Benchmark
    @Threads(1)
    public void singleThread_getThenPut_mixed(final MapState state, final ThreadState thread, final Blackhole bh) {
        final int idx = thread.nextIndex(state);
        final Object key = state.keys[idx];
        final Object value = thread.nextWriteValue(state, idx);

        bh.consume(state.map.get(key));
        bh.consume(state.map.put(key, value));
    }

    @Benchmark
    @Threads(Threads.MAX)
    public void concurrent_getThenPut_mixed(final MapState state, final ThreadState thread, final Blackhole bh) {
        final int idx = thread.nextIndex(state);
        final Object key = state.keys[idx];
        final Object value = thread.nextWriteValue(state, idx);

        bh.consume(state.map.get(key));
        bh.consume(state.map.put(key, value));
    }

    private static final class ResourceKey {
        private final String namespace;
        private final String path;
        private final int hash;

        ResourceKey(final int id) {
            // Small namespace space (like many mods sharing a few namespaces), unique paths.
            this.namespace = "mod" + (id & 1023);
            this.path = "model/" + id;

            int h = namespace.hashCode();
            h = 31 * h + path.hashCode();
            this.hash = h;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ResourceKey)) {
                return false;
            }
            final ResourceKey other = (ResourceKey) obj;
            return namespace.equals(other.namespace) && path.equals(other.path);
        }
    }
}
