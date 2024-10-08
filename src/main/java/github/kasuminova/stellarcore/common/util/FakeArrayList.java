package github.kasuminova.stellarcore.common.util;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * 这是一个假的 ArrayList，内部使用其他 List 封装。
 */
@SuppressWarnings({"StandardVariableNames", "unused", "CloneableClassInSecureContext"})
public abstract class FakeArrayList<E> extends ArrayList<E> {

    protected final List<E> internal;

    public FakeArrayList(final List<E> internal, final int initialCapacity) {
        super(0);
        this.internal = internal;
    }

    public FakeArrayList(final List<E> internal) {
        this.internal = internal;
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
    public boolean contains(final Object o) {
        return internal.contains(o);
    }

    @Override
    public int indexOf(final Object o) {
        return internal.indexOf(o);
    }

    @Override
    public int lastIndexOf(final Object o) {
        return internal.lastIndexOf(o);
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return internal.toArray();
    }

    @Nonnull
    @Override
    public <T> T[] toArray(final T[] a) {
        return internal.toArray(a);
    }

    @Override
    public E get(final int index) {
        return internal.get(index);
    }

    @Override
    public E set(final int index, final E element) {
        return internal.set(index, element);
    }

    @Override
    public boolean add(final E e) {
        return internal.add(e);
    }

    @Override
    public void add(final int index, final E element) {
        internal.add(index, element);
    }

    @Override
    public E remove(final int index) {
        return internal.remove(index);
    }

    @Override
    public boolean remove(final Object o) {
        return internal.remove(o);
    }

    @Override
    public void clear() {
        internal.clear();
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        return internal.addAll(c);
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends E> c) {
        return internal.addAll(index, c);
    }

    @Override
    protected void removeRange(final int fromIndex, final int toIndex) {
        ListIterator<E> it = internal.listIterator(fromIndex);
        for (int i = 0, n = toIndex - fromIndex; i < n; i++) {
            it.next();
            it.remove();
        }
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return internal.removeAll(c);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return internal.retainAll(c);
    }

    @Nonnull
    @Override
    public ListIterator<E> listIterator(final int index) {
        return internal.listIterator(index);
    }

    @Nonnull
    @Override
    public ListIterator<E> listIterator() {
        return internal.listIterator();
    }

    @Nonnull
    @Override
    public Iterator<E> iterator() {
        return internal.iterator();
    }

    @Nonnull
    @Override
    public List<E> subList(final int fromIndex, final int toIndex) {
        return internal.subList(fromIndex, toIndex);
    }

    @Override
    public void forEach(final Consumer<? super E> action) {
        internal.forEach(action);
    }

    @Override
    public Spliterator<E> spliterator() {
        return internal.spliterator();
    }

    @Override
    public boolean removeIf(final Predicate<? super E> filter) {
        return internal.removeIf(filter);
    }

    @Override
    public void replaceAll(final UnaryOperator<E> operator) {
        internal.replaceAll(operator);
    }

    @Override
    public void sort(final Comparator<? super E> c) {
        internal.sort(c);
    }

    @Override
    public boolean containsAll(@Nonnull final Collection<?> c) {
        return internal.containsAll(c);
    }

    @Override
    public Stream<E> stream() {
        return internal.stream();
    }

    @Override
    public Stream<E> parallelStream() {
        return internal.parallelStream();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof List)) {
            return false;
        }

        return this.internal.equals(o);
    }

    @Override
    public int hashCode() {
        return internal.hashCode();
    }

}
