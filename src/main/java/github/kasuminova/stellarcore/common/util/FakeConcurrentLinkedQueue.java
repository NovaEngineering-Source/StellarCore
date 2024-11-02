package github.kasuminova.stellarcore.common.util;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("StandardVariableNames")
public class FakeConcurrentLinkedQueue<E> extends ConcurrentLinkedQueue<E> {

    private final Queue<E> internal;

    public FakeConcurrentLinkedQueue(final Collection<? extends E> c, @Nonnull final Queue<E> internal) {
        this.internal = internal;
    }

    public FakeConcurrentLinkedQueue(@Nonnull final Queue<E> internal) {
        this.internal = internal;
    }

    @Override
    public void forEach(final Consumer<? super E> action) {
        internal.forEach(action);
    }

    @Override
    public void clear() {
        internal.clear();
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return internal.retainAll(c);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return internal.removeAll(c);
    }

    @Override
    public boolean removeIf(final Predicate<? super E> filter) {
        return internal.removeIf(filter);
    }

    @Override
    public Spliterator<E> spliterator() {
        return internal.spliterator();
    }

    @Nonnull
    @Override
    public Iterator<E> iterator() {
        return internal.iterator();
    }

    @Nonnull
    @Override
    public <T> T[] toArray(final T[] a) {
        return internal.toArray(a);
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return internal.toArray();
    }

    @Override
    public String toString() {
        return internal.toString();
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        return internal.addAll(c);
    }

    @Override
    public boolean remove(final Object o) {
        return internal.remove(o);
    }

    @Override
    public boolean contains(final Object o) {
        return internal.contains(o);
    }

    @Override
    public int size() {
        return internal.size();
    }

    @Override
    public boolean isEmpty() {
        return internal.isEmpty();
    }

    @Override
    public E peek() {
        return internal.peek();
    }

    @Override
    public E poll() {
        return internal.poll();
    }

    @Override
    public boolean offer(final E element) {
        return internal.offer(element);
    }

    @Override
    public boolean add(final E element) {
        return internal.add(element);
    }

}
