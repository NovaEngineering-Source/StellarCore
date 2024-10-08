package github.kasuminova.stellarcore.common.util;

import github.kasuminova.stellarcore.mixin.util.StellarNBTTagList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.NBTBase;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@SuppressWarnings({"NonFinalClone", "MethodDoesntCallSuperMethod", "CloneableClassInSecureContext"})
public class NBTTagBackingList extends FakeArrayList<NBTBase> {

    private StellarNBTTagList changeHandler = null;

    public NBTTagBackingList(final int initialCapacity) {
        super(new ObjectArrayList<>(), initialCapacity);
    }

    public NBTTagBackingList() {
        super(new ObjectArrayList<>());
    }

    public NBTTagBackingList(final Collection<? extends NBTBase> c) {
        super(new ObjectArrayList<>(c));
    }

    public void setChangeHandler(final StellarNBTTagList changeHandler) {
        this.changeHandler = changeHandler;
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        return super.toArray();
    }

    @Nonnull
    @Override
    public <T> T[] toArray(final T[] a) {
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        return super.toArray(a);
    }

    @Override
    public NBTBase set(final int index, final NBTBase element) {
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        return super.set(index, element);
    }

    @Override
    public boolean add(final NBTBase e) {
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        return super.add(e);
    }

    @Override
    public void add(final int index, final NBTBase element) {
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        super.add(index, element);
    }

    @Override
    public NBTBase remove(final int index) {
        final NBTBase removed = super.remove(index);
        if (removed != null && changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        return removed;
    }

    @Override
    public boolean remove(final Object o) {
        final boolean removed = super.remove(o);
        if (removed && changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        return removed;
    }

    @Override
    public void clear() {
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        super.clear();
    }

    @Override
    public boolean addAll(final Collection<? extends NBTBase> c) {
        if (!c.isEmpty() && changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        return super.addAll(c);
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends NBTBase> c) {
        if (!c.isEmpty() && changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        return super.addAll(index, c);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        final boolean removed = super.removeAll(c);
        if (removed && changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        return removed;
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        final boolean retained = super.retainAll(c);
        if (retained && changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        return retained;
    }

    @Nonnull
    @Override
    public ListIterator<NBTBase> listIterator(final int index) {
        return new TagListListIterator(super.listIterator(index), changeHandler);
    }

    @Nonnull
    @Override
    public ListIterator<NBTBase> listIterator() {
        return new TagListListIterator(super.listIterator(), changeHandler);
    }

    @Nonnull
    @Override
    public Iterator<NBTBase> iterator() {
        return new TagListIterator(super.iterator(), changeHandler);
    }

    public Iterator<NBTBase> unwrappedIterator() {
        return super.iterator();
    }

    public ListIterator<NBTBase> unwrappedListIterator(final int index) {
        return super.listIterator(index);
    }

    public ListIterator<NBTBase> unwrappedListIterator() {
        return super.listIterator();
    }

    @Nonnull
    @Override
    public List<NBTBase> subList(final int fromIndex, final int toIndex) {
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        return super.subList(fromIndex, toIndex);
    }

    @Override
    public boolean removeIf(final Predicate<? super NBTBase> filter) {
        final boolean removed = super.removeIf(filter);
        if (removed && changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        return removed;
    }

    @Override
    public void replaceAll(final UnaryOperator<NBTBase> operator) {
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        super.replaceAll(operator);
    }

    @Override
    public void sort(final Comparator<? super NBTBase> c) {
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
        super.sort(c);
    }

    @Override
    public Object clone() {
        NBTTagBackingList cloned = new NBTTagBackingList(internal);
        cloned.changeHandler = null;
        return cloned;
    }

}
