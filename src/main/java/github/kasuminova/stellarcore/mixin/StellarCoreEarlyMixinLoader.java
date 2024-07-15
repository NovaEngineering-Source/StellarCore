package github.kasuminova.stellarcore.mixin;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

@SuppressWarnings("unused")
public class StellarCoreEarlyMixinLoader implements IFMLLoadingPlugin {
    public static final Logger LOG = LogManager.getLogger("STELLAR_CORE");
    public static final String LOG_PREFIX = "[STELLAR_CORE]" + ' ';
    private static final Map<String, BooleanSupplier> MIXIN_CONFIGS = new LinkedHashMap<>();

    static {
        addMixinCFG("mixins.stellar_core_minecraft_advancements.json",  () -> StellarCoreConfig.FEATURES.vanilla.asyncAdvancementSerialize);
        addMixinCFG("mixins.stellar_core_minecraft_chunk.json",         () -> StellarCoreConfig.PERFORMANCE.vanilla.blockPos2ValueMap);
        addMixinCFG("mixins.stellar_core_minecraft_longnbtkiller.json", () -> StellarCoreConfig.BUG_FIXES.vanilla.longNBTKiller);
        addMixinCFG("mixins.stellar_core_minecraft_nnlist.json",        () -> StellarCoreConfig.PERFORMANCE.vanilla.nonNullList);
        addMixinCFG("mixins.stellar_core_minecraft_noglerror.json",     () -> StellarCoreConfig.PERFORMANCE.vanilla.noGlError);
        addMixinCFG("mixins.stellar_core_minecraft_renderglobal.json",  () -> StellarCoreConfig.PERFORMANCE.vanilla.alwaysDeferChunkUpdates);
        addMixinCFG("mixins.stellar_core_minecraft_world.json",         () -> StellarCoreConfig.PERFORMANCE.vanilla.capturedBlockSnapshots);
        addMixinCFG("mixins.stellar_core_forge.json",                   () -> StellarCoreConfig.PERFORMANCE.customLoadingScreen.splashProgress);
        addMixinCFG("mixins.stellar_core_forge_asmdatatable.json",      () -> StellarCoreConfig.PERFORMANCE.forge.asmDataTable);
        addMixinCFG("mixins.stellar_core_hudcaching.json",              () -> StellarCoreConfig.PERFORMANCE.vanilla.hudCaching);

        addMixinCFG("mixins.stellar_core_forge_modelloader.json");
        addMixinCFG("mixins.stellar_core_minecraft_statemapperbase.json");
    }

    private static void addMixinCFG(final String mixinConfig) {
        MIXIN_CONFIGS.put(mixinConfig, () -> true);
    }

    private static void addMixinCFG(final String mixinConfig, final BooleanSupplier conditions) {
        MIXIN_CONFIGS.put(mixinConfig, conditions);
    }

    public static boolean isCleanroomLoader() {
        try {
            Class.forName("com.cleanroommc.boot.Main");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // Noop

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(final Map<String, Object> data) {
        MIXIN_CONFIGS.forEach((config, supplier) -> {
            if (supplier == null) {
                LOG.warn(LOG_PREFIX + "Mixin config {} is not found in config map! It will never be loaded.", config);
                return;
            }
            boolean shouldLoad = supplier.getAsBoolean();
            if (!shouldLoad) {
                LOG.info(LOG_PREFIX + "Mixin config {} is disabled by config or mod is not loaded.", config);
                return;
            }
            Mixins.addConfiguration(config);
        });
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
