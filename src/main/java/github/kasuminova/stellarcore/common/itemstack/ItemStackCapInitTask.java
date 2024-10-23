package github.kasuminova.stellarcore.common.itemstack;

import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.StellarItemStack;
import net.minecraft.item.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("DataFlowIssue")
public class ItemStackCapInitTask implements Runnable {

    private final StellarItemStack target;

    private final AtomicBoolean done = new AtomicBoolean(false);
    private final AtomicBoolean joined = new AtomicBoolean(false);

    private volatile Thread working = null;
    private volatile Thread joining = null;

    public ItemStackCapInitTask(final ItemStack target) {
        this.target = (StellarItemStack) (Object) target;
    }

    @Override
    public void run() {
        if (done.get() || working != null) {
            return;
        }

        synchronized (this) {
            if (working != null) {
                return;
            }

            working = Thread.currentThread();

            try {
                target.stellar_core$initCap();
            } catch (Throwable e) {
                StellarLog.LOG.warn("[StellarCore-ItemStackCapInitTask] Failed to execute capability init task!", e);
            }

            working = null;
            done.set(true);
        }
    }

    public boolean isDone() {
        return done.get();
    }

    public boolean join() {
        if (joined.get()) {
            return true;
        }

        final Thread current = Thread.currentThread();
        // Recursion check.
        if (this.joining == current || this.working == current) {
            return false;
        }

        // Lock the target instead of the task itself.
        synchronized (this) {
            // Recursion check.
            if (this.joining == current || this.working == current) {
                return false;
            }
            // Wait for another thread finish.
            awaitJoinComplete();
            // Set the joining thread.
            this.joining = current;
        }

        if (!done.get()) {
            run();
        }
        if (!joined.get()) {
            target.stellar_core$joinCapInit();
            joined.set(true);
        }

        this.joining = null;
        return true;
    }

    private void awaitJoinComplete() {
        // Wait for the task to finish.
        while (this.joining != null) {
            Thread.yield();
        }
    }

}
