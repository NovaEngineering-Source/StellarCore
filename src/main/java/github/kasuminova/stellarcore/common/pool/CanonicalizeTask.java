package github.kasuminova.stellarcore.common.pool;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CanonicalizeTask<T> {
    
    private final Supplier<T> task;
    private final Consumer<T> callback;

    public CanonicalizeTask(final Supplier<T> task, @Nullable final Consumer<T> callback) {
        this.task = task;
        this.callback = callback;
    }

    public void execute() {
        T result = task.get();
        if (callback != null) {
            callback.accept(result);
        }
    }

}
