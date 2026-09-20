package github.kasuminova.stellarcore.shaded.org.jctools.util;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

public final class Int2ObjectPair<V> extends Pair<Integer, V> {

    private final int k,hashCode;
    private final V v;

    public Int2ObjectPair(int k,V v) {
        this.k = k;
        this.v = v;
        this.hashCode = k * 31 + Objects.hashCode(v);
    }

    public int getIntLeft() {
        return k;
    }

    @Override
    @Deprecated
    public Integer getLeft() {
        return k;
    }

    @Override
    public V getRight() {
        return v;
    }

    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Int2ObjectPair<?> that = (Int2ObjectPair<?>) o;
        return k == that.k && Objects.equals(v, that.v);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
