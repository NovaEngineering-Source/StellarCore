package github.kasuminova.stellarcore.client.integration.ctm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import team.chisel.ctm.client.util.BlockstatePredicateParser;

import java.util.Map;
import java.util.function.BiPredicate;

/** Shares read-only CTM connection rules within one resource generation. */
public final class ConnectionPredicateCache {

    private static volatile NonBlockingHashMap<String, BiPredicate<EnumFacing, IBlockState>> cache = new NonBlockingHashMap<>();
    private static final ThreadLocal<BlockstatePredicateParser> PARSERS = ThreadLocal.withInitial(BlockstatePredicateParser::new);

    private ConnectionPredicateCache() {
    }

    public static BiPredicate<EnumFacing, IBlockState> parse(final JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return null;
        }
        // Content keys also observe edits to existing metadata objects. JsonElement.equals
        // is unsuitable: numeric JSON values with different string forms can compare equal.
        final String key = json.toString();
        final NonBlockingHashMap<String, BiPredicate<EnumFacing, IBlockState>> current = cache;
        BiPredicate<EnumFacing, IBlockState> result = current.get(key);
        if (result != null) {
            return result;
        }
        try {
            // CTM removes "default" and "compare_func" while parsing. Keep the shared
            // metadata intact, and never let a failed parse poison another thread's context.
            result = PARSERS.get().parse(copy(json));
        } catch (RuntimeException | Error failure) {
            PARSERS.remove();
            throw failure;
        }
        final BiPredicate<EnumFacing, IBlockState> previous = current.putIfAbsent(key, result);
        return previous == null ? result : previous;
    }

    public static void clear() {
        // Loads already in progress can finish against their old generation without
        // repopulating the new generation's cache.
        cache = new NonBlockingHashMap<>();
    }

    private static JsonElement copy(final JsonElement json) {
        // JsonElement.deepCopy is not public in the Gson shipped with Forge 1.12.2.
        if (json.isJsonObject()) {
            final JsonObject copy = new JsonObject();
            for (final Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                copy.add(entry.getKey(), copy(entry.getValue()));
            }
            return copy;
        }
        if (json.isJsonArray()) {
            final JsonArray copy = new JsonArray();
            for (final JsonElement element : json.getAsJsonArray()) {
                copy.add(copy(element));
            }
            return copy;
        }
        return json;
    }
}
