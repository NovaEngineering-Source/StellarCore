package github.kasuminova.stellarcore.common.util;

import com.github.bsideup.jabel.Desugar;
import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings({"CloneableClassInSecureContext", "UnusedReturnValue"})
public class BlockPos2IntMap implements Object2IntMap<BlockPos> {

    protected final Long2IntOpenHashMap internal = new Long2IntOpenHashMap();
    protected EntrySet entrySet = null;
    protected KeySet keySet = null;

    @Nonnull
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ObjectSet<Map.Entry<BlockPos, Integer>> entrySet() {
        return (ObjectSet) (entrySet == null ? entrySet = new EntrySet() : entrySet);
    }

    @Override
    public ObjectSet<Object2IntMap.Entry<BlockPos>> object2IntEntrySet() {
        return entrySet == null ? entrySet = new EntrySet() : entrySet;
    }

    @Nonnull
    @Override
    public ObjectSet<BlockPos> keySet() {
        return keySet == null ? keySet = new KeySet() : keySet;
    }

    @Nonnull
    @Override
    public IntCollection values() {
        return internal.values();
    }

    @Override
    public boolean containsValue(final int value) {
        return internal.containsValue(value);
    }

    @Override
    public int put(final BlockPos key, final int value) {
        return internal.put(key.toLong(), value);
    }

    @Override
    public int getInt(final Object key) {
        if (key instanceof BlockPos pos) {
            return internal.get(pos.toLong());
        }
        return defaultReturnValue();
    }

    @Override
    public int removeInt(final Object key) {
        if (key instanceof BlockPos pos) {
            return internal.remove(pos.toLong());
        }
        return defaultReturnValue();
    }

    @Override
    public void defaultReturnValue(final int rv) {
        internal.defaultReturnValue(rv);
    }

    @Override
    public int defaultReturnValue() {
        return internal.defaultReturnValue();
    }

    @Override
    public Integer put(final BlockPos key, final Integer value) {
        return internal.put(key.toLong(), (int) value);
    }

    @Override
    public Integer get(final Object key) {
        if (key instanceof BlockPos pos) {
            return internal.get(pos.toLong());
        }
        return 0;
    }

    @Override
    public boolean containsKey(final Object key) {
        if (key instanceof BlockPos pos) {
            return internal.containsKey(pos.toLong());
        }
        return false;
    }

    @Override
    public boolean containsValue(final Object value) {
        if (value instanceof Integer i) {
            return internal.containsValue(i);
        }
        return false;
    }

    @Override
    public Integer remove(final Object key) {
        if (key instanceof BlockPos pos) {
            return internal.remove(pos.toLong());
        }
        return 0;
    }

    @Override
    public void putAll(final Map<? extends BlockPos, ? extends Integer> map) {
        map.forEach((key, v) -> internal.put(key.toLong(), (int) v));
    }

    public int addTo(final BlockPos pos, final int incr) {
        return internal.addTo(pos.toLong(), incr);
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
    public void clear() {
        internal.clear();
    }

    public class EntrySet extends AbstractObjectSet<Object2IntMap.Entry<BlockPos>> {

        @Override
        public int size() {
            return internal.size();
        }

        @Override
        public void clear() {
            internal.clear();
        }

        @Nonnull
        @Override
        public ObjectIterator<Object2IntMap.Entry<BlockPos>> iterator() {
            Iterator<Entry> transformed = Iterators.transform(internal.long2IntEntrySet().iterator(), Entry::new);
            return new EntryIterator(transformed);
        }

    }

    @Desugar
    private record KeyIterator(EntryIterator parent) implements ObjectIterator<BlockPos> {

        @Override
        public int skip(final int n) {
            return parent.skip(n);
        }

        @Override
        public boolean hasNext() {
            return parent.hasNext();
        }

        @Override
        public BlockPos next() {
            return parent.next().getKey();
        }

    }

    @Desugar
    private record EntryIterator(Iterator<Entry> transformed) implements ObjectIterator<Object2IntMap.Entry<BlockPos>> {

        @Override
        public int skip(final int n) {
            int skipped = 0;
            int i = 0;
            while (i < n && hasNext()) {
                transformed.next();
                skipped++;
                i++;
            }
            return skipped;
        }

        @Override
        public boolean hasNext() {
            return transformed.hasNext();
        }

        @Override
        public Object2IntMap.Entry<BlockPos> next() {
            return transformed.next();
        }

    }

    public class KeySet extends AbstractObjectSet<BlockPos> {

        @Nonnull
        @Override
        @SuppressWarnings("RedundantCast")
        public ObjectIterator<BlockPos> iterator() {
            return new KeyIterator((EntryIterator) (Object) (entrySet().iterator()));
        }

        @Override
        public int size() {
            return internal.size();
        }

    }

    @SuppressWarnings("deprecation")
    public static class Entry implements Object2IntMap.Entry<BlockPos> {

        private final Long2IntMap.Entry parent;

        public Entry(Long2IntMap.Entry parent) {
            this.parent = parent;
        }

        @Override
        public BlockPos getKey() {
            return BlockPos.fromLong(parent.getLongKey());
        }

        @Override
        public Integer getValue() {
            return parent.getValue();
        }

        @Override
        public int setValue(final int value) {
            return parent.setValue(value);
        }

        @Override
        public int getIntValue() {
            return parent.getValue();
        }

        @Override
        public Integer setValue(final Integer value) {
            return parent.setValue(value);
        }
    }

}
