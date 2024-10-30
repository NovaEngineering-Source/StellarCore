package github.kasuminova.stellarcore.common.itemstack;

import github.kasuminova.stellarcore.mixin.util.StellarItemStack;
import net.minecraft.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("DataFlowIssue")
public class ItemStackCapInitTask {

    private static final ThreadLocal<Deque<ItemStackCapInitTask>> JOINING_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private final StellarItemStack target;

    private boolean asyncComponentInitialized = false;
    private AtomicBoolean done;
    private AtomicBoolean joining;

    private ReentrantLock loadLock;
    private ReentrantLock joinLock;

    public ItemStackCapInitTask(final ItemStack target) {
        this.target = (StellarItemStack) (Object) target;
    }

    void initAsyncComponents() {
        this.done = new AtomicBoolean(false);
        this.joining = new AtomicBoolean(false);
        this.loadLock = new ReentrantLock();
        this.joinLock = new ReentrantLock();
        this.asyncComponentInitialized = true;
    }

    public boolean tryRun() {
        // Current: JoinLock = locked / unlocked, LoadLock = unlocked
        if (acquireLoadLock()) {
            // Acquired LoadLock, start running
            // Current: JoinLock = locked / unlocked, LoadLock = locked
            if (!done.get()) {
                run();
                done.set(true);
            }

            // Unlock LoadLock
            // Current: JoinLock = locked / unlocked, LoadLock = unlocked
            releaseLoadLock();
            return true;
        } else {
            return false;
        }
    }

    public void run() {
        target.stellar_core$initCap();
    }

    public boolean isDone() {
        return done.get();
    }

    public boolean join() {
        final Deque<ItemStackCapInitTask> taskStack = JOINING_STACK.get();
        // Recursion check
        if (taskStack.contains(this)) {
            return false;
        }
        taskStack.push(this);

        try {
            if (asyncComponentInitialized) {
                // Current: JoinLock = locked
                acquireJoinLock();

                if (tryRun()) {
                    target.stellar_core$joinCapInit();

                    // Current: JoinLock = unlocked
                    releaseJoinLock();
                } else {
                    // Current: JoinLock = unlocked
                    releaseJoinLock();
                    // If we cannot acquire LoadLock, wait for the another thread to finish load.
                    awaitLoadComplete();

                    target.stellar_core$joinCapInit();
                }
            } else {
                run();
            }
        } finally {
            taskStack.pop();
        }

        return true;
    }

    private void acquireJoinLock() {
        while (true) {
            awaitJoinComplete();

            if (joinLock.tryLock()) {
                // Check again.
                if (!joining.get()) {
                    // Acquired lock.
                    joining.set(true);
                    joinLock.unlock();
                    break;
                }
                // Acquire failed, unlock and wait for the another thread, then retry.
                // Join cannot be interrupted.
                joinLock.unlock();
            }
        }
    }

    private void awaitJoinComplete() {
        // Wait for the another thread finish
        while (joining.get()) {
            Thread.yield();
        }
    }

    private void releaseJoinLock() {
        // Release lock
        joinLock.lock();
        joining.set(false);
        joinLock.unlock();
    }

    private boolean acquireLoadLock() {
        return loadLock.tryLock();
    }

    private void releaseLoadLock() {
        loadLock.unlock();
    }

    private void awaitLoadComplete() {
        while (loadLock.isLocked()) {
            // Wait for the another thread finish
            Thread.yield();
        }
    }

}
