package github.kasuminova.stellarcore.common.pool;

import github.kasuminova.stellarcore.common.util.StellarLog;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public abstract class AsyncCanonicalizePool<T> extends AsyncCanonicalizePoolBase<T> {

    public static final long CLEAR_TIMEOUT_MS = 10_000L;

    private final AtomicInteger locked = new AtomicInteger(0);

    protected volatile long processedCount = 0;

    private volatile Future<Void> clearTask = null;

    public final T canonicalize(@Nullable final T target) {
        if (target == null) {
            return null;
        }

        T processed = preProcess(target);
        locked.incrementAndGet();

        Set<T> pool = getPoolKeySet();
        synchronized (pool) {
            T canonicalized = null;
            try {
                canonicalized = canonicalizeInternal(processed);
            } catch (Throwable e) {
                StellarLog.LOG.error("Pool {} caught a error while canonicalizing `{}`.", getName(), target);
                StellarLog.LOG.error(e);
            }
            locked.decrementAndGet();
            processedCount++;
            return canonicalized;
        }
    }

    protected T preProcess(final T target) {
        return target;
    }

    protected abstract T canonicalizeInternal(final T target);

    public synchronized void clear() {
        if (clearTask != null) {
            if (!clearTask.isDone()) {
                return;
            }
            clearTask = null;
        }

        clearTask = CompletableFuture.runAsync(() -> {
            long start = System.currentTimeMillis();
            while (locked.get() > 0) {
                if (System.currentTimeMillis() - start > CLEAR_TIMEOUT_MS) {
                    StellarLog.LOG.warn("Pool {} Wait for task completion timeout (over {}ms), force clearing.", getName(), CLEAR_TIMEOUT_MS);
                    break;
                }
                LockSupport.parkNanos(1_000_000L); // 1ms
            }
            onClearPre();

            processedCount = 0;
            Set<T> pool = getPoolKeySet();
            synchronized (pool) {
                locked.set(0);
                clearPool();
            }

            onClearPost();
            clearTask = null;
        });
    }

    protected abstract void clearPool();

    public void onClearPre() {
    }

    public void onClearPost() {
    }

    public long getProcessedCount() {
        return processedCount;
    }

    public int getUniqueCount() {
        return getPoolKeySet().size();
    }

    protected abstract Set<T> getPoolKeySet();

}
