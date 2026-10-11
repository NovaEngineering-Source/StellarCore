package github.kasuminova.stellarcore.client.model;

import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;

import java.util.Map;

public final class ModelMapTransfer {

    private ModelMapTransfer() {
    }

    public static <K, V> void copy(final Map<K, V> source, final Map<? super K, ? super V> destination) {
        if (source instanceof NonBlockingHashMap<?, ?>) {
            ((NonBlockingHashMap<K, V>) source).forEachSnapshot(destination::put);
        } else {
            source.forEach(destination::put);
        }
    }
}
