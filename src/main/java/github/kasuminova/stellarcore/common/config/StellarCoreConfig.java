package github.kasuminova.stellarcore.common.config;

import com.cleanroommc.configanytime.ConfigAnytime;
import github.kasuminova.stellarcore.StellarCore;
import github.kasuminova.stellarcore.client.model.ParallelModelLoaderAsyncBlackList;
import github.kasuminova.stellarcore.client.pool.StellarUnpackedDataPool;
import github.kasuminova.stellarcore.common.config.category.BugFixes;
import github.kasuminova.stellarcore.common.config.category.Debug;
import github.kasuminova.stellarcore.common.config.category.Features;
import github.kasuminova.stellarcore.common.config.category.Performance;
import github.kasuminova.stellarcore.common.entity.EntityForceUpdateManager;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;

@Mod.EventBusSubscriber(modid = StellarCore.MOD_ID)
@Config(modid = StellarCore.MOD_ID, name = StellarCore.MOD_ID)
public class StellarCoreConfig {

    @Config.Name("Debug")
    public static final Debug DEBUG = new Debug();

    @Config.Name("BugFixes")
    public static final BugFixes BUG_FIXES = new BugFixes();

    @Config.Name("Performance")
    public static final Performance PERFORMANCE = new Performance();

    @Config.Name("Features")
    public static final Features FEATURES = new Features();

    /*
        必须在最后加载。
     */
    static {
        ConfigAnytime.register(StellarCoreConfig.class);
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(StellarCore.MOD_ID)) {
            ConfigManager.sync(StellarCore.MOD_ID, Config.Type.INSTANCE);

            EntityForceUpdateManager.INSTANCE.reload();
            if (FMLLaunchHandler.side().isClient()) {
                ParallelModelLoaderAsyncBlackList.INSTANCE.reload();
                // Pool does not reference minecraft class, so is safety.
                if (StellarUnpackedDataPool.update()) {
                    StellarUnpackedDataPool.reset();
                }
            }
        }
    }

}
