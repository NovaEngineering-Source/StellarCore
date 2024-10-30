package github.kasuminova.stellarcore.common.util;

import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class BlockPosSet implements Set<BlockPos> {

    protected final LongSet internal = new LongOpenHashSet();

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
        if (o instanceof BlockPos) {
            return internal.contains(((BlockPos) o).toLong());
        }
        return false;
    }

    @Nonnull
    @Override
    public Iterator<BlockPos> iterator() {
        return Iterators.transform(internal.iterator(), BlockPos::fromLong);
    }

    @Nonnull
    @Override
    public BlockPos[] toArray() {
        final BlockPos[] arr = new BlockPos[internal.size()];
        final LongIterator it = internal.iterator();
        int i = 0;
        while (it.hasNext()) {
            arr[i] = BlockPos.fromLong(it.nextLong());
            i++;
        }
        return arr;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(final T[] a) {
        if (a.length < internal.size()) {
            return (T[]) toArray();
        }
        final LongIterator it = internal.iterator();
        int i = 0;
        while (it.hasNext()) {
            a[i] = (T) BlockPos.fromLong(it.nextLong());
            i++;
        }
        return a;
    }

    @Override
    public boolean add(final BlockPos pos) {
        return internal.add(pos.toLong());
    }

    @Override
    public boolean remove(final Object o) {
        if (o instanceof BlockPos) {
            return internal.remove(((BlockPos) o).toLong());
        }
        return false;
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        for (final Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(final Collection<? extends BlockPos> c) {
        for (final BlockPos pos : c) {
            add(pos);
        }
        return !c.isEmpty();
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        boolean modified = false;
        Iterator<BlockPos> it = iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        boolean modified = false;
        for (final Object o : c) {
            modified = remove(o);
        }
        return modified;
    }

    @Override
    public void clear() {
        internal.clear();
    }

}
