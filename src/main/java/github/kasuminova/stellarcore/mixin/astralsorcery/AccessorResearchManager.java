package github.kasuminova.stellarcore.mixin.astralsorcery;

import hellfirepvp.astralsorcery.common.data.research.PlayerProgress;
import hellfirepvp.astralsorcery.common.data.research.ResearchManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

@Mixin(ResearchManager.class)
public interface AccessorResearchManager {

    @Nonnull
    @Accessor(remap = false)
    @SuppressWarnings("DataFlowIssue")
    static Map<UUID, PlayerProgress> getPlayerProgressServer() {
        return null;
    }

}
