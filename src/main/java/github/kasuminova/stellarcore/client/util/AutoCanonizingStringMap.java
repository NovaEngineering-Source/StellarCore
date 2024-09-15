package github.kasuminova.stellarcore.client.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import zone.rong.loliasm.api.LoliStringPool;

import java.util.Map;

@SuppressWarnings({"CloneableClassInSecureContext", "AssignmentToMethodParameter", "unchecked", "ChainOfInstanceofChecks"})
public class AutoCanonizingStringMap<V> extends Object2ObjectOpenHashMap<String, V> {

    public AutoCanonizingStringMap() {
    }

    public AutoCanonizingStringMap(Map<String, V> map) {
        super(map);
    }

    @Override
    public V put(String key, V value) {
        if (key instanceof String) {
            key = LoliStringPool.canonicalize(key);
        }
        if (value instanceof String) {
            value = (V) LoliStringPool.canonicalize((String) value);
        }
        return super.put(key, value);
    }

}
