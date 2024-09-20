package github.kasuminova.stellarcore.common.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;

public class FastClearIdentityHashMap<K, V> extends IdentityHashMap<K, V> {

    private static final MethodHandle setTable;

    static {
        try {
            Field tableField = IdentityHashMap.class.getDeclaredField("table");
            tableField.setAccessible(true);
            setTable = MethodHandles.lookup().unreflectSetter(tableField);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public FastClearIdentityHashMap() {
    }

    public FastClearIdentityHashMap(int expectedMaxSize) {
        super(expectedMaxSize);
    }

    @Override
    public void clear() {
        try {
            setTable.invoke(this, new Object[4]);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        super.clear();
    }

}
