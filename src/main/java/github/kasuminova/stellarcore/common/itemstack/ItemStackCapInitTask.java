package github.kasuminova.stellarcore.common.itemstack;

import github.kasuminova.stellarcore.mixin.util.StellarItemStackCapLoader;
import net.minecraft.item.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("DataFlowIssue")
public class ItemStackCapInitTask implements Runnable {

    private final StellarItemStackCapLoader target;
    private final AtomicBoolean done = new AtomicBoolean(false);

    public ItemStackCapInitTask(final ItemStack target) {
        this.target = (StellarItemStackCapLoader) (Object) target;
    }

    @Override
    public synchronized void run() {
        if (done.get()) {
            return;
        }
        target.stellar_core$initCap();
        done.set(true);
    }

    public boolean isDone() {
        return done.get();
    }

    public void join() {
        if (!isDone()) {
            run();
        }
        target.stellar_core$joinCapInit();
    }

}
