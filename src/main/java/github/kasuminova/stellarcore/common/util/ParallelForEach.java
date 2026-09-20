package github.kasuminova.stellarcore.common.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class ParallelForEach {

    private ParallelForEach() {
    }

    public static <T> void balanced(final T[] indexed, final Consumer<T> action) {
        final int size = indexed.length;
        if (size == 0) {
            return;
        }

        final int parallelism = Math.min(size, ForkJoinPool.getCommonPoolParallelism() + 1);
        if (parallelism <= 1) {
            for (T t : indexed) {
                action.accept(t);
            }
            return;
        }

        final AtomicInteger cursor = new AtomicInteger();
        final Runnable worker = () -> {
            int index;
            while ((index = cursor.getAndIncrement()) < size) {
                action.accept(indexed[index]);
            }
        };

        final ForkJoinTask<?>[] tasks = new ForkJoinTask<?>[parallelism - 1];
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = ForkJoinPool.commonPool().submit(worker);
        }
        worker.run();
        for (ForkJoinTask<?> task : tasks) {
            task.join();
        }
    }

    public static <T> void balanced(final List<T> elements, final Consumer<T> action) {
        final List<T> indexed = elements instanceof RandomAccess ? elements : new ObjectArrayList<>(elements);
        final int size = indexed.size();
        if (size == 0) {
            return;
        }

        final int parallelism = Math.min(size, ForkJoinPool.getCommonPoolParallelism() + 1);
        if (parallelism <= 1) {
            for (int i = 0; i < size; i++) {
                action.accept(indexed.get(i));
            }
            return;
        }

        final AtomicInteger cursor = new AtomicInteger();
        final Runnable worker = () -> {
            int index;
            while ((index = cursor.getAndIncrement()) < size) {
                action.accept(indexed.get(index));
            }
        };

        final ForkJoinTask<?>[] tasks = new ForkJoinTask<?>[parallelism - 1];
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = ForkJoinPool.commonPool().submit(worker);
        }
        worker.run();
        for (ForkJoinTask<?> task : tasks) {
            task.join();
        }
    }

}
