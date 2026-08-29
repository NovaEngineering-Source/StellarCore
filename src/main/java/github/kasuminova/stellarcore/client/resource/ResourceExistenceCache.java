package github.kasuminova.stellarcore.client.resource;

import java.util.Map;

/** Cache policies shared by resource packs with different mutability guarantees. */
public final class ResourceExistenceCache {

    private ResourceExistenceCache() {
    }

    public static <K> boolean rememberMutable(final Map<K, Boolean> cache, final K key, final boolean exists) {
        if (exists) {
            cache.putIfAbsent(key, Boolean.TRUE);
        }
        return exists;
    }

    public static <K> boolean rememberImmutable(final Map<K, Boolean> cache, final K key, final boolean exists) {
        final Boolean previous = cache.putIfAbsent(key, exists);
        return previous == null ? exists : previous;
    }
}
