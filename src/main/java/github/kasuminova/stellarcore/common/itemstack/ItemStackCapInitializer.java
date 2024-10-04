package github.kasuminova.stellarcore.common.itemstack;

import github.kasuminova.stellarcore.common.util.StellarEnvironment;
import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.shaded.org.jctools.queues.atomic.MpmcAtomicArrayQueue;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class ItemStackCapInitializer implements Runnable {

    public static final ItemStackCapInitializer INSTANCE = new ItemStackCapInitializer();

    private static final int QUEUE_BOUND_SIZE = 50_000;
    private static final int MAX_WORKERS = Math.min(StellarEnvironment.getConcurrency(), 8);

    private static final ThreadLocal<Long> PARKED_MICROS = ThreadLocal.withInitial(() -> 0L);

    private final Queue<ItemStackCapInitTask> taskQueue = createConcurrentQueue();

    private final AtomicLong completedTasks = new AtomicLong();
    private final AtomicInteger queueSize = new AtomicInteger();
    private final List<Thread> workers = new CopyOnWriteArrayList<>();

    private long start = System.currentTimeMillis();
    private long completedTasksOffset = 0;

    private ItemStackCapInitializer() {
        createWorkerInternal();
    }

    public static void resetStatus() {
        INSTANCE.completedTasksOffset = -INSTANCE.completedTasks.get();
        INSTANCE.start = System.currentTimeMillis();
    }

    public static long getExistedMillis() {
        return System.currentTimeMillis() - INSTANCE.start;
    }

    public static long getCompletedTasks() {
        return INSTANCE.completedTasks.get() + INSTANCE.completedTasksOffset;
    }

    public static int getQueueSize() {
        return INSTANCE.queueSize.get();
    }

    public static int getMaxQueueSize() {
        return MAX_WORKERS * QUEUE_BOUND_SIZE;
    }

    public static int getWorkers() {
        return INSTANCE.workers.size();
    }

    public static int getMaxWorkers() {
        return MAX_WORKERS;
    }

    private synchronized void createWorker() {
        int tasks = queueSize.get();
        int workers = this.workers.size();
        if (tasks < (QUEUE_BOUND_SIZE * workers) || workers > MAX_WORKERS) {
            return;
        }
        StellarLog.LOG.warn("[StellarCore-ItemStackCapInitializer] Creating new worker because queue size reached bound size (limit {}).", QUEUE_BOUND_SIZE * workers);
        createWorkerInternal();
    }

    private void createWorkerInternal() {
        Thread worker = new Thread(this, "StellarCore-ItemStackCapInitializer-" + workers.size());
        worker.start();
        worker.setPriority(Thread.NORM_PRIORITY + 1);
        workers.add(worker);
    }

    public void addTask(final ItemStackCapInitTask task) {
        while (!taskQueue.offer(task)) {
            if (taskQueue.offer(task)) {
                break;
            }
        }

        int tasks = queueSize.incrementAndGet();
        int workers = this.workers.size();
        if ((workers > 0) && (tasks > (QUEUE_BOUND_SIZE * workers)) && (workers < MAX_WORKERS)) {
            createWorker();
        }
    }

    @Override
    public void run() {
        StellarLog.LOG.info("[StellarCore] {} started.", Thread.currentThread().getName());
        while (!Thread.currentThread().isInterrupted()) {
            ItemStackCapInitTask task;
            boolean executed = false;

            try {
                int completed = 0;
                while ((task = pollTask()) != null) {
                    task.run();
                    executed = true;
                    completed++;
                    queueSize.decrementAndGet();
                }
                completedTasks.addAndGet(completed);
            } catch (Throwable e) {
                StellarLog.LOG.error("[StellarCore] ItemStackCapInitializer failed to execute task.", e);
            }

            if (executed) {
                PARKED_MICROS.set(0L);
            }
            park();
        }
        StellarLog.LOG.warn("[StellarCore] {} stopped, it may be invalid.", Thread.currentThread().getName());
    }

    private ItemStackCapInitTask pollTask() {
        return taskQueue.poll();
    }

    private static void park() {
        long parkedMicros = PARKED_MICROS.get();
        if (parkedMicros > 100_000) { // 100ms
            LockSupport.parkNanos(500_000L); // 0.5ms
            PARKED_MICROS.set(parkedMicros + 500);
        } else if (parkedMicros > 50_000) { // 50ms
            LockSupport.parkNanos(100_000L); // 0.1ms
            PARKED_MICROS.set(parkedMicros + 100);
        } else if (parkedMicros > 10_000) { // 10ms
            LockSupport.parkNanos(10_000L); // 10μs
            PARKED_MICROS.set(parkedMicros + 10);
        } else {
            LockSupport.parkNanos(5_000L); // 5μs
            PARKED_MICROS.set(parkedMicros + 5);
        }
    }

    private static <E> Queue<E> createConcurrentQueue() {
        return new MpmcAtomicArrayQueue<>(QUEUE_BOUND_SIZE * (MAX_WORKERS + 1));
    }

}
