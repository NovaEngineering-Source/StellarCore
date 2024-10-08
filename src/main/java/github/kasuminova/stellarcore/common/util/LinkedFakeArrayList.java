package github.kasuminova.stellarcore.common.util;

import java.util.Collection;
import java.util.LinkedList;

/**
 * 这是一个假的 ArrayList，内部使用 LinkedList 封装。
 */
@SuppressWarnings({"StandardVariableNames", "unused", "CloneableClassInSecureContext", "MethodDoesntCallSuperMethod", "NonFinalClone"})
public class LinkedFakeArrayList<E> extends FakeArrayList<E> {

    public LinkedFakeArrayList(final int initialCapacity) {
        super(new LinkedList<>(), initialCapacity);
    }

    public LinkedFakeArrayList() {
        super(new LinkedList<>());
    }

    public LinkedFakeArrayList(final Collection<? extends E> c) {
        super(new LinkedList<>(c));
    }

    @Override
    public Object clone() {
        return new LinkedFakeArrayList<>(internal);
    }

}
