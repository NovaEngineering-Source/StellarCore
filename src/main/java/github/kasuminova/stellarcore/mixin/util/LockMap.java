package github.kasuminova.stellarcore.mixin.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockMap {

    private static final Map<Object, Lock> LOCK_MAP = new ConcurrentHashMap<>();

    public static Lock getLock(Object key) {
        return LOCK_MAP.computeIfAbsent(key, k -> new ReentrantLock());
    }

    public static void lock(Object key) {
        getLock(key).lock();
    }

    public static void unlock(Object key) {
        getLock(key).unlock();
    }

    public static void clear() {
        LOCK_MAP.clear();
    }

}
