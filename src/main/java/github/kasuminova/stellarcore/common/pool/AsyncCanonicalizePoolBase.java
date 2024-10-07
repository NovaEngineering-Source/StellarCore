package github.kasuminova.stellarcore.common.pool;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public abstract class AsyncCanonicalizePoolBase<T> {

    protected final CanonicalizeWorker<T> worker = new CanonicalizeWorker<>(getName());

    public AsyncCanonicalizePoolBase() {
        worker.start();
    }

    public void canonicalizeAsync(final T target, final Consumer<T> callback) {
        worker.offer(new CanonicalizeTask<>(() -> canonicalize(target), callback));
    }

    public void canonicalizeDeferred(final DeferredCanonicalizable<T> target) {
        worker.defer(target);
    }

    public abstract T canonicalize(@Nullable final T target);

    public CanonicalizeWorker<T> getWorker() {
        return worker;
    }

    public abstract long getProcessedCount();

    public abstract int getUniqueCount();

    public abstract void clear();

    protected abstract String getName();

}
