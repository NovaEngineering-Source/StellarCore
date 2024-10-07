package github.kasuminova.stellarcore.common.pool;

import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.shaded.org.jctools.queues.MpscUnboundedArrayQueue;

import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.locks.LockSupport;

public class CanonicalizeWorker<T> implements Runnable {

    private final Queue<WeakReference<DeferredCanonicalizable<T>>> deferedQueue = createConcurrentQueue();
    private final Queue<CanonicalizeTask<T>> queue = createConcurrentQueue();
    private final String name;

    private volatile Thread worker = null;
    private long parkedMillis = 0;

    public CanonicalizeWorker(final String name) {
        this.name = name;
    }

    public void start() {
        if (isRunning()) {
            return;
        }
        stop();
        worker = new Thread(this, "StellarCore-" + name + "-CanonicalizeWorker");
        worker.start();
    }

    public void stop() {
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
    }

    public boolean isRunning() {
        return worker != null && worker.isAlive();
    }

    public Thread getThread() {
        return worker;
    }

    public void offer(final CanonicalizeTask<T> task) {
        queue.offer(task);
    }

    public void defer(final DeferredCanonicalizable<T> canonicalizable) {
        deferedQueue.offer(new WeakReference<>(canonicalizable));
    }

    @Override
    public void run() {
        StellarLog.LOG.info("[StellarCore-{}] CanonicalizeWorker started.", name);
        while (!Thread.currentThread().isInterrupted()) {
            WeakReference<DeferredCanonicalizable<T>> ref;
            while ((ref = deferedQueue.poll()) != null) {
                DeferredCanonicalizable<T> canonicalizable = ref.get();
                if (canonicalizable != null) {
                    canonicalizable.canonicalizeAsync();
                }
            }

            CanonicalizeTask<T> task;
            boolean executed = false;
            while ((task = queue.poll()) != null) {
                task.execute();
                executed = true;
            }

            if (executed) {
                parkedMillis = 0;
            }

            park();
        }
        StellarLog.LOG.info("[StellarCore-{}] CanonicalizeWorker stopped.", name);
    }

    private void park() {
        if (parkedMillis > 10_000) {
            LockSupport.parkNanos(100_000_000L); // 100ms
            parkedMillis += 100;
        } else if (parkedMillis > 1_000) {
            LockSupport.parkNanos(10_000_000L); // 10ms
            parkedMillis += 10;
        } else {
            LockSupport.parkNanos(1_000_000L); // 1ms
            parkedMillis += 1;
        }
    }

    private static <E> Queue<E> createConcurrentQueue() {
        return new MpscUnboundedArrayQueue<>(10_000);
    }

}
