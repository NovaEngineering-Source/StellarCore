package github.kasuminova.stellarcore.common.itemstack;

import github.kasuminova.stellarcore.mixin.util.StellarItemStack;
import net.minecraft.item.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("DataFlowIssue")
public class ItemStackCapInitTask {

    private static final ThreadLocal<Boolean> CAPABILITY_JOINING = ThreadLocal.withInitial(() -> false);

    private final StellarItemStack target;

    private boolean asyncComponentInitialized = false;
    private AtomicBoolean done;
    private AtomicBoolean joining;

    private Lock loadLock;
    private Lock joinLock;

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
        if (loadLock.tryLock()) {
            if (!done.get()) {
                run();
                done.set(true);
            }
            loadLock.unlock();
            return true;
        }
        return false;
    }

    public void run() {
        target.stellar_core$initCap();
    }

    public boolean isDone() {
        return done.get();
    }

    public boolean join() {
        // Recursion check
        if (CAPABILITY_JOINING.get()) {
            return true;
        }
        CAPABILITY_JOINING.set(true);

        if (asyncComponentInitialized) {
            acquireJoinLock();

            // If another thread is currently running.
            if (!tryRun()) {
                releaseJoinLock();
                CAPABILITY_JOINING.set(false);
                return false;
            }
            target.stellar_core$joinCapInit();

            releaseJoinLock();
        } else {
            run();
        }

        CAPABILITY_JOINING.set(false);
        return true;
    }

    private void acquireJoinLock() {
        while (true) {
            // Wait for the another thread finish
            while (joining.get()) {
                Thread.yield();
            }

            joinLock.lock();
            // Check again
            if (!joining.get()) {
                // Acquired lock
                joining.set(true);
                joinLock.unlock();
                break;
            }
            joinLock.unlock();
        }
    }

    private void releaseJoinLock() {
        // Release lock
        joinLock.lock();
        joining.set(false);
        joinLock.unlock();
    }

}
