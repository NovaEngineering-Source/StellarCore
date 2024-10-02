package github.kasuminova.stellarcore.client.util;

import github.kasuminova.stellarcore.common.mod.Mods;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import mirror.normalasm.api.NormalStringPool;
import net.minecraftforge.fml.common.Optional;
import zone.rong.loliasm.api.LoliStringPool;

import java.util.Map;
import java.util.function.Function;

@SuppressWarnings({"CloneableClassInSecureContext", "AssignmentToMethodParameter", "unchecked"})
public class AutoCanonizingStringMap<V> extends Object2ObjectOpenHashMap<String, V> {
    
    private final Function<String, String> canonicalizer;

    public AutoCanonizingStringMap() {
        canonicalizer = initCanonicalizer();
    }

    public AutoCanonizingStringMap(Map<String, V> map) {
        super(map);
        canonicalizer = initCanonicalizer();
    }

    @Override
    public V put(String key, V value) {
        key = canonicalizer.apply(key);
        if (value instanceof String) {
            value = (V) canonicalizer.apply((String) value);
        }
        return super.put(key, value);
    }

    private static Function<String, String> initCanonicalizer() {
        if (Mods.CENSORED_ASM.loaded()) {
            return AutoCanonizingStringMap::canonicalizeFromLoliStringPool;
        } else if (Mods.FERMIUM_OR_BLAHAJ_ASM.loaded()) {
            return AutoCanonizingStringMap::canonicalizeFromNormalStringPool;
        } else {
            throw new IllegalStateException("No valid StringPool implementation found!");
        }
    }

    @Optional.Method(modid = "loliasm")
    private static String canonicalizeFromLoliStringPool(final String target) {
        return LoliStringPool.canonicalize(target);
    }

    @Optional.Method(modid = "normalasm")
    private static String canonicalizeFromNormalStringPool(final String target) {
        return NormalStringPool.canonicalize(target);
    }

}
