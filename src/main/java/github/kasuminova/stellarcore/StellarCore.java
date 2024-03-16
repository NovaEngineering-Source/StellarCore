package github.kasuminova.stellarcore;

import github.kasuminova.stellarcore.common.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import org.apache.logging.log4j.Logger;

@Mod(modid = StellarCore.MOD_ID, name = StellarCore.MOD_NAME, version = StellarCore.VERSION,
        dependencies = "required-after:forge@[14.23.5.2847,);" +
                "required-after:theoneprobe@[1.12-1.4.28,);" +
                "after:jianghun@[1.0,);",
        acceptedMinecraftVersions = "[1.12, 1.13)"
)
@SuppressWarnings("MethodMayBeStatic")
public class StellarCore {
    public static final String MOD_ID = "stellarcore";
    public static final String MOD_NAME = "Stellar Core";

    public static final String VERSION = Tags.VERSION;

    public static final String CLIENT_PROXY = "github.kasuminova.stellarcore.client.ClientProxy";
    public static final String COMMON_PROXY = "github.kasuminova.stellarcore.common.CommonProxy";

    @Mod.Instance(MOD_ID)
    public static StellarCore instance = null;
    @SidedProxy(clientSide = CLIENT_PROXY, serverSide = COMMON_PROXY)
    public static CommonProxy proxy = null;
    public static Logger log = null;

    @Mod.EventHandler
    public void construction(FMLConstructionEvent event) {
        proxy.construction();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        event.getModMetadata().version = VERSION;
        log = event.getModLog();
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        proxy.loadComplete();
    }
}
