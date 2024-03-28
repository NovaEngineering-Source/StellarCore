package github.kasuminova.stellarcore.common.config;

import com.cleanroommc.configanytime.ConfigAnytime;
import github.kasuminova.stellarcore.StellarCore;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = StellarCore.MOD_ID)
@Config(modid = StellarCore.MOD_ID, name = StellarCore.MOD_ID)
public class StellarCoreConfig {

    static {
        ConfigAnytime.register(StellarCoreConfig.class);
    }

    @Config.Name("BugFixes")
    public static final BugFixes BUG_FIXES = new BugFixes();

    @Config.Name("Performance")
    public static final Performance PERFORMANCE = new Performance();

    @Config.Name("Features")
    public static final Features FEATURES = new Features();

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(StellarCore.MOD_ID)) {
            ConfigManager.sync(StellarCore.MOD_ID, Config.Type.INSTANCE);
        }
    }

    public static class BugFixes {

        @Config.Name("ArmourersWorkshop")
        public final ArmourersWorkshop armourersWorkshop = new ArmourersWorkshop();

        @Config.Name("AstralSorcery")
        public final AstralSorcery astralSorcery = new AstralSorcery();

        @Config.Name("DraconicEvolution")
        public final DraconicEvolution draconicEvolution = new DraconicEvolution();

        @Config.Name("EnderIOConduits")
        public final EnderIOConduits enderIOConduits = new EnderIOConduits();

        @Config.Name("FluxNetworks")
        public final FluxNetworks fluxNetworks = new FluxNetworks();

        @Config.Name("IndustrialCraft2")
        public final IndustrialCraft2 industrialCraft2 = new IndustrialCraft2();

        @Config.Name("InGameInfoXML")
        public final InGameInfoXML inGameInfoXML = new InGameInfoXML();

        @Config.Name("ImmersiveEngineering")
        public final ImmersiveEngineering immersiveEngineering = new ImmersiveEngineering();

        @Config.Name("MoreElectricTools")
        public final MoreElectricTools moreElectricTools = new MoreElectricTools();

        @Config.Name("MrCrayfishFurniture")
        public final MrCrayfishFurniture mrCrayfishFurniture = new MrCrayfishFurniture();

        @Config.Name("ScalingGuis")
        public final ScalingGuis scalingGuis = new ScalingGuis();

        @Config.Name("Sync")
        public final Sync sync = new Sync();

        @Config.Name("Techguns")
        public final Techguns techguns = new Techguns();

        @Config.Name("TheOneProbe")
        public final TheOneProbe theOneProbe = new TheOneProbe();

        @Config.Name("ThermalDynamics")
        public final ThermalDynamics thermalDynamics = new ThermalDynamics();

        public static class ArmourersWorkshop {
            @Config.Name("SkinTextureCrashFixes")
            public boolean skinTexture = true;
        }

        public static class AstralSorcery {

            @Config.RequiresWorldRestart
            @Config.Name("PlayerAttributeMapCrashFixes")
            public boolean playerAttributeMap = true;

        }

        public static class DraconicEvolution {

            @Config.Name("CraftingInjectorFixes")
            public boolean craftingInjector = true;

        }

        public static class EnderIOConduits {

            @Config.Name("ItemConduitItemStackCache")
            public boolean cachedItemConduit = true;

        }

        public static class FluxNetworks {

            @Config.Name("TheOneProbeIntegration")
            public boolean fixTop = true;

        }

        public static class IndustrialCraft2 {

            @Config.Name("GradualRecipeFixes")
            public boolean gradualRecipe = true;

        }

        public static class InGameInfoXML {

            @Config.Name("PlayerHandlerFixes")
            public boolean playerHandler = true;

        }

        public static class ImmersiveEngineering {

            @Config.Name("MultiblockStructureContainerFixes")
            public boolean blockIEMultiblock = true;

            @Config.Name("JerryCanFixes")
            public boolean fixJerryCanRecipe = true;

        }

        public static class MoreElectricTools {

            @Config.Name("LifeSupportsFixes")
            public boolean fixLifeSupports = true;

        }

        public static class MrCrayfishFurniture {

            @Config.RequiresMcRestart
            @Config.Name("ImageCacheCrashFixes")
            public boolean imageCache = true;

            @Config.Name("RotatableFurniture")
            public boolean rotatableFurniture = true;

            @Config.Name("BlockFurnitureTileFixes")
            public boolean blockFurnitureTile = true;
        }

        public static class ScalingGuis {

            @Config.Name("JsonHelperCrashFixes")
            public boolean fixJsonHelper = true;

        }

        public static class Sync {

            @Config.Name("TechgunsDuplicationFixes")
            public boolean techgunsDuplicationFixes = true;

            @Config.Name("RidingFixes")
            public boolean ridingFixes = true;

        }

        public static class Techguns {

            @Config.Name("TGPermissionsCrashFixes")
            public boolean tgPermissions = true;

            @Config.Name("InvalidRecipeFixes")
            public boolean fixAmmoSumRecipeFactory = true;

        }

        public static class TheOneProbe {

            @Config.Name("PlayerEntityRenderFixes")
            public boolean fixRenderHelper = true;

        }

        public static class ThermalDynamics {

            @Config.Name("FluidDuplicateFixes")
            public boolean fixFluidDuplicate = true;

        }

    }

    public static class Performance {

        @Config.Name("Vanilla")
        public final Vanilla vanilla = new Vanilla();

        @Config.Name("Avaritia")
        public final Avaritia avaritia = new Avaritia();

        @Config.Name("BiomesOPlenty")
        public final BiomesOPlenty biomesOPlenty = new BiomesOPlenty();

        @Config.Name("ExtraBotany")
        public final ExtraBotany extraBotany = new ExtraBotany();

        @Config.Name("BloodMagic")
        public final BloodMagic bloodMagic = new BloodMagic();

        @Config.Name("Botania")
        public final Botania botania = new Botania();

        @Config.Name("Chisel")
        public final Chisel chisel = new Chisel();

        @Config.Name("Cucumber")
        public final Cucumber cucumber = new Cucumber();

        @Config.Name("CustomLoadingScreen")
        public final CustomLoadingScreen customLoadingScreen = new CustomLoadingScreen();

        @Config.Name("EnderCore")
        public final EnderCore enderCore = new EnderCore();

        @Config.Name("EnderIO")
        public final EnderIO enderIO = new EnderIO();

        @Config.Name("EnderIOConduits")
        public final EnderIOConduits enderIOConduits = new EnderIOConduits();

        @Config.Name("IndustrialCraft2")
        public final IndustrialCraft2 industrialCraft2 = new IndustrialCraft2();

        @Config.Name("InGameInfoXML")
        public final InGameInfoXML inGameInfoXML = new InGameInfoXML();

        @Config.Name("Mekanism")
        public final Mekanism mekanism = new Mekanism();

        public static class Vanilla {

            @Config.RequiresMcRestart
            @Config.Name("CapturedBlockSnapshotsImprovements")
            public boolean capturedBlockSnapshots = false;

            @Config.RequiresMcRestart
            @Config.Name("ChunkTileEntityMapImprovements")
            public boolean blockPos2ValueMap = true;

        }

        public static class Avaritia {

            @Config.Name("TileBaseImprovements")
            public boolean tileBase = true;

        }

        public static class BiomesOPlenty {

            @Config.Name("TrailManagerAsync")
            public boolean trailManager = true;

        }

        public static class Cucumber {

            @Config.Name("VanillaPacketDispatcherImprovements")
            public boolean vanillaPacketDispatcher = false;

            @Config.Name("TileEntityUpdateRange")
            public float tileEntityUpdateRange = 16F;

        }

        public static class ExtraBotany {

            @Config.Name("PersistentVariableHandlerAsync")
            public boolean persistentVariableHandler = true;

        }

        public static class BloodMagic {

            @Config.Name("BloodAltarImprovements")
            public boolean bloodAltar = true;

        }

        public static class Botania {

            @Config.Name("SparkEntityImprovements")
            public boolean sparkImprovements = true;

            @Config.RangeInt(min = 2, max = 60)
            @Config.Name("SparkMaxWorkDelay")
            public int sparkMaxWorkDelay = 10;

        }

        public static class Chisel {

            @Config.Name("AutoChiselImprovements")
            public boolean autoChiselImprovements = true;

            @Config.RangeInt(min = 20, max = 100)
            @Config.Name("AutoChiselMaxWorkDelay")
            public int autoChiselMaxWorkDelay = 100;

        }

        public static class CustomLoadingScreen {

            @Config.RequiresMcRestart
            @Config.Name("TextureCleanup")
            public boolean splashProgress = true;

        }

        public static class EnderCore {

            @Config.Name("OreThingImprovements")
            public boolean oreThing = true;

        }

        public static class EnderIO {

            @Config.Name("ItemToolsImprovements")
            public boolean itemTools = true;

            @Config.Name("TileEntityBaseImprovements")
            public boolean tileEntityBase = true;

        }

        public static class EnderIOConduits {

            @Config.Name("AbstractConduitImprovements")
            public boolean abstractConduit = true;

            @Config.Name("TileConduitBundleImprovements")
            public boolean tileConduitBundle = true;

        }

        public static class IndustrialCraft2 {

            @Config.Name("EnergyCalculatorLegImprovements")
            public boolean energyCalculatorLeg = true;

            @Config.Name("GridImprovements")
            public boolean grid = true;

            @Config.Name("ItemUpgradeModuleImprovements")
            public boolean itemUpgradeModule = false;

            @Config.Name("ItemUpgradeModuleWorkDelay")
            public int itemUpgradeModuleWorkDelay = 5;

        }

        public static class InGameInfoXML {

            @Config.Name("HUDFramebuffer")
            public boolean hudFrameBuffer = true;

            @Config.Name("HUDFPS")
            @Config.RangeInt(min = 5, max = 60)
            public int hudFrameRate = 10;

        }

        public static class Mekanism {

            @Config.Name("PipeUtilsImprovements")
            public boolean pipeUtils = true;

            @Config.Name("EnergyNetworkImprovements")
            public boolean energyNetwork = true;

            @Config.Name("FrequencyImprovements")
            public boolean frequency = true;

        }

    }

    public static class Features {
        @Config.Name("EnableCustomGameTitle")
        public boolean enableTitle = false;

        @Config.Name("TitleUseHitokotoAPI")
        public boolean hitokoto = true;

        @Config.Name("CustomGameTitle")
        public String title = "Minecraft 1.12.2";

        @Config.Name("FontScale")
        public final FontScale fontScale = new FontScale();

        @Config.Name("AstralSorcery")
        public final AstralSorcery astralSorcery = new AstralSorcery();

        @Config.Name("BetterChat")
        public final BetterChat betterChat = new BetterChat();

        @Config.Name("Botania")
        public final Botania botania = new Botania();

        @Config.Name("LegendaryTooltips")
        public final LegendaryTooltips legendaryTooltips = new LegendaryTooltips();

        @Config.Name("Mekanism")
        public final Mekanism mekanism = new Mekanism();

        @Config.Name("NuclearCraftOverhauled")
        public final NuclearCraftOverhauled nuclearCraftOverhauled = new NuclearCraftOverhauled();

        @Config.Name("RGBChat")
        public final RGBChat rgbChat = new RGBChat();

        @Config.Name("Techguns")
        public final Techguns techguns = new Techguns();

        @Config.Name("MoreElectricTools")
        public final MoreElectricTools moreElectricTools = new MoreElectricTools();

        public static class FontScale {

            @Config.RangeDouble(min = 0F, max = 1.0F)
            @Config.Name("AppliedEnergetics2")
            public float ae2 = 0.5F;

        }

        public static class AstralSorcery {

            @Config.Name("DisableChainMining")
            public boolean disableChainMining = false;

        }

        public static class BetterChat {

            @Config.Name("EnableMessageCompat")
            public boolean messageCompat = false;

        }

        public static class Botania {

            @Config.Name("DisableCosmeticRecipe")
            public boolean disableCosmeticRecipe = false;

        }

        public static class LegendaryTooltips {

            @Config.Name("DisableTitleWrap")
            public boolean tooltipDecor = false;

        }

        public static class Mekanism {

            @Config.RequiresMcRestart
            @Config.Name("TOPSupport")
            public boolean topSupport = true;

            @Config.Name("FluxNetworksSupport")
            public boolean fluxNetworksSupport = true;

        }

        public static class NuclearCraftOverhauled {

            @Config.Name("DisableRadiationCapability")
            public boolean removeRadiationCapabilityHandler = false;

        }

        public static class RGBChat {

            @Config.Name("TrueRGBSimpleRendererImprovements")
            public boolean cachedRGBFontRenderer = true;

        }

        public static class Techguns {

            @Config.Name("ForceSecurityMode")
            public boolean forceSecurityMode = true;

            @Config.Name("BulletIsProjectile")
            public boolean tgDamageSource = true;

        }

        public static class MoreElectricTools {

            @Config.Name("RemoveEfficientEnergyCostEnchantment")
            public boolean disableEfficientEnergyCost = false;

        }

    }

}
