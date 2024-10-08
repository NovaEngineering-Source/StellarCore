package github.kasuminova.stellarcore.common.pool;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import javax.annotation.Nullable;

public class StringPool extends AsyncCanonicalizePoolBase<String> {

    public static final StringPool INSTANCE = new StringPool();

    private final ObjectOpenHashSet<String> pool = new ObjectOpenHashSet<>();
    private volatile long processedCount = 0;

    private StringPool() {
    }

    @Override
    public String canonicalize(@Nullable final String target) {
        if (target == null) {
            return null;
        }
        synchronized (pool) {
            processedCount++;
            return pool.addOrGet(target);
        }
    }

    @Override
    public long getProcessedCount() {
        return processedCount;
    }

    @Override
    public int getUniqueCount() {
        return pool.size();
    }

    @Override
    protected String getName() {
        return "StringPool";
    }

    @Override
    public void clear() {
        synchronized (pool) {
            processedCount = 0;
            pool.clear();
        }
    }

}
