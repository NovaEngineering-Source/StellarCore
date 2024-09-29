package github.kasuminova.stellarcore.common.itemstack;

import github.kasuminova.stellarcore.common.util.StellarLog;
import io.netty.util.internal.shaded.org.jctools.queues.atomic.MpscLinkedAtomicQueue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

public class ItemStackCapInitializer implements Runnable {

    public static final ItemStackCapInitializer INSTANCE = new ItemStackCapInitializer();
    
    private final Queue<ItemStackCapInitTask> taskQueue = createConcurrentQueue();

    private long parkedMicros = 0;
    
    private final Thread worker;

    private ItemStackCapInitializer() {
        worker = new Thread(this, "StellarCore-ItemStackCapInitializer");
        worker.start();
    }

    public void addTask(final ItemStackCapInitTask task) {
        taskQueue.offer(task);
    }

    @Override
    public void run() {
        StellarLog.LOG.info("[StellarCore] ItemStackCapInitializer started.");
        while (!Thread.currentThread().isInterrupted()) {
            ItemStackCapInitTask task;
            boolean executed = false;

            try {
                while ((task = taskQueue.poll()) != null) {
                    task.run();
                    executed = true;
                }
            } catch (Throwable e) {
                StellarLog.LOG.error("[StellarCore] ItemStackCapInitializer failed to execute task.", e);
            }

            if (executed) {
                parkedMicros = 0;
            }
            park();
        }
        StellarLog.LOG.warn("[StellarCore] ItemStackCapInitializer stopped, it may be invalid.");
    }

    private void park() {
        if (parkedMicros > 100_000) {
            LockSupport.parkNanos(500_000L); // 500μs
            parkedMicros += 500;
        } else if (parkedMicros > 10_000) {
            LockSupport.parkNanos(100_000L); // 100μs
            parkedMicros += 100;
        } else {
            LockSupport.parkNanos(10_000L); // 10μs
            parkedMicros += 10;
        }
    }

    private static <E> Queue<E> createConcurrentQueue() {
        try {
            // May be incompatible with cleanroom.
            return new MpscLinkedAtomicQueue<>();
        } catch (Throwable e) {
            return new ConcurrentLinkedQueue<>();
        }
    }

}
