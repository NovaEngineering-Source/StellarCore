package github.kasuminova.stellarcore.common.itemstack;

import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.StellarItemStack;
import net.minecraft.item.ItemStack;

@SuppressWarnings("DataFlowIssue")
public class ItemStackCapInitTask implements Runnable {

    private final StellarItemStack target;
    private volatile boolean done = false;

    public ItemStackCapInitTask(final ItemStack target) {
        this.target = (StellarItemStack) (Object) target;
    }

    @Override
    public synchronized void run() {
        if (done) {
            return;
        }
        try {
            target.stellar_core$initCap();
        } catch (Throwable e) {
            StellarLog.LOG.warn("[StellarCore-ItemStackCapInitTask] Failed to execute capability init task!", e);
        }
        done = true;
    }

    public boolean isDone() {
        return done;
    }

    public void join() {
        if (!done) {
            run();
        }
        target.stellar_core$joinCapInit();
    }

}
