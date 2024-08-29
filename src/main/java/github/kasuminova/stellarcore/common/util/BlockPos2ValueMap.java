package github.kasuminova.stellarcore.common.util;

import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.*;

public class BlockPos2ValueMap<V> implements Map<BlockPos, V> {

    protected final Long2ObjectMap<V> internal = new Long2ObjectLinkedOpenHashMap<>();
    protected EntrySet entrySet = null;
    protected KeySet keySet = null;

    @Override
    public int size() {
        return internal.size();
    }

    @Override
    public boolean isEmpty() {
        return internal.isEmpty();
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
        return internal.containsValue(value);
    }

    @Override
    public V get(final Object key) {
        if (key instanceof BlockPos pos) {
            return internal.get(pos.toLong());
        }
        return null;
    }

    @Override
    public V put(final BlockPos key, final V value) {
        return internal.put(key.toLong(), value);
    }

    @Override
    public V remove(final Object key) {
        if (key instanceof BlockPos pos) {
            return internal.remove(pos.toLong());
        }
        return null;
    }

    @Override
    public void putAll(final Map<? extends BlockPos, ? extends V> map) {
        map.forEach((k, v) -> internal.put(k.toLong(), v));
    }

    @Override
    public void clear() {
        internal.clear();
    }

    @Nonnull
    @Override
    public Set<BlockPos> keySet() {
        return keySet == null ? keySet = new KeySet() : keySet;
    }

    @Nonnull
    @Override
    public Collection<V> values() {
        return internal.values();
    }

    @Nonnull
    @Override
    public Set<Map.Entry<BlockPos, V>> entrySet() {
        return entrySet == null ? entrySet = new EntrySet() : entrySet;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof BlockPos2ValueMap<?> map) {
            return internal.equals(map.internal);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return internal.hashCode();
    }

    public class EntrySet extends AbstractSet<Map.Entry<BlockPos, V>> {
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
        public Iterator<Map.Entry<BlockPos, V>> iterator() {
            return Iterators.transform(internal.long2ObjectEntrySet().iterator(), Entry::new);
        }
    }

    public class KeySet extends AbstractSet<BlockPos> {

        @Nonnull
        @Override
        public Iterator<BlockPos> iterator() {
            return Iterators.transform(entrySet().iterator(), Map.Entry::getKey);
        }

        @Override
        public int size() {
            return internal.size();
        }

    }

    public static class Entry<V> implements Map.Entry<BlockPos, V> {
        private final Long2ObjectMap.Entry<V> parent;

        public Entry(Long2ObjectMap.Entry<V> parent) {
            this.parent = parent;
        }

        @Override
        public BlockPos getKey() {
            return BlockPos.fromLong(parent.getLongKey());
        }

        @Override
        public V getValue() {
            return parent.getValue();
        }

        @Override
        public V setValue(final V value) {
            return parent.setValue(value);
        }
    }
}
