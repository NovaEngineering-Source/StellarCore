package github.kasuminova.stellarcore.common.util;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.client.resources.FallbackResourceManager;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Desugar
public record PublishedState(Map<String, FallbackResourceManager> managers, Set<String> domains) {
    private static final PublishedState EMPTY = new PublishedState(
        Collections.emptyMap(), Collections.emptySet()
    );

    public static PublishedState empty() {
        return EMPTY;
    }
}