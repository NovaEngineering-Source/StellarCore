package github.kasuminova.stellarcore.client.integration.libnine;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.mod.Mods;
import github.kasuminova.stellarcore.common.util.StellarLog;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;

import java.util.Set;

public class L9ModScanner {

    private static final Set<String> VALID_MODS = new ObjectOpenHashSet<>();

    public static boolean isValidMod(final String modId) {
        return VALID_MODS.contains(modId);
    }

    public static void scan() {
        if (!Mods.LIB_NINE.loaded()) {
            return;
        }
        if (!StellarCoreConfig.PERFORMANCE.vanilla.resourceExistStateCache) {
            return;
        }
        if (!StellarCoreConfig.PERFORMANCE.libNine.l9ModelsIsOfTypeCache) {
            return;
        }
        StellarLog.LOG.info("[StellarCore-L9ModScanner] L9Model filter is enabled.");

        VALID_MODS.clear();
        VALID_MODS.add("libnine");
        Loader.instance().getActiveModList().forEach(mod -> {
            if (mod.getRequirements().stream().map(ArtifactVersion::getLabel).anyMatch("libnine"::equalsIgnoreCase)) {
                VALID_MODS.add(mod.getModId());
            }
        });

        StellarLog.LOG.info("[StellarCore-L9ModScanner] Valid mods for LibNine mods: {}", VALID_MODS);
    }

}
