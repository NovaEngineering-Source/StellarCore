package github.kasuminova.stellarcore.common.config;

import com.cleanroommc.configanytime.ConfigAnytime;
import github.kasuminova.stellarcore.StellarCore;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = StellarCore.MOD_ID)
@Config(modid = StellarCore.MOD_ID, name = "stellar_core")
public class StellarCoreConfig {

    static {
        ConfigAnytime.register(StellarCoreConfig.class);
    }

    @Config.Name("Bug Fixes")
    public static final BugFixes BUG_FIXES = new BugFixes();

    @Config.Name("Performance")
    public static final Performance PERFORMANCE = new Performance();

    @Config.Name("Features")
    public static final Features FEATURES = new Features();

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(StellarCore.MOD_ID))
        {
            ConfigManager.sync(StellarCore.MOD_ID, Config.Type.INSTANCE);
        }
    }

    public static class BugFixes {

        @Config.Name("Armourers Workshop")
        public final ArmourersWorkshop armourersWorkshop = new ArmourersWorkshop();

        @Config.Name("AstralSorcery")
        public final AstralSorcery astralSorcery = new AstralSorcery();

        @Config.Name("MrCrayfish Furniture")
        public final MrCrayfishFurniture mrCrayfishFurniture = new MrCrayfishFurniture();

        @Config.Name("EnderIO Conduits")
        public final EnderIOConduits enderIOConduits = new EnderIOConduits();

        @Config.Name("Flux Networks")
        public final FluxNetworks fluxNetworks = new FluxNetworks();

        @Config.Name("IndustrialCraft2")
        public final IndustrialCraft2 industrialCraft2 = new IndustrialCraft2();

        @Config.Name("InGameInfoXML")
        public final InGameInfoXML inGameInfoXML = new InGameInfoXML();

        @Config.Name("Immersive Engineering")
        public final ImmersiveEngineering immersiveEngineering = new ImmersiveEngineering();

        @Config.Name("More Electric Tools")
        public final MoreElectricTools moreElectricTools = new MoreElectricTools();

        @Config.Name("Scaling Guis")
        public final ScalingGuis scalingGuis = new ScalingGuis();

        @Config.Name("Techguns")
        public final Techguns techguns = new Techguns();

        @Config.Name("TheOneProbe")
        public final TheOneProbe theOneProbe = new TheOneProbe();

        @Config.Name("Thermal Dynamics")
        public final ThermalDynamics thermalDynamics = new ThermalDynamics();

        public static class ArmourersWorkshop {
            @Config.Name("SkinTexture Crash Fixes")
            public boolean skinTexture = true;
        }

        public static class AstralSorcery {

            @Config.RequiresWorldRestart
            @Config.Name("PlayerAttributeMap Crash Fixes")
            public boolean playerAttributeMap = true;

        }

        public static class MrCrayfishFurniture {

            @Config.RequiresMcRestart
            @Config.Name("ImageCache Crash Fixes")
            public boolean imageCache = true;

        }

        public static class EnderIOConduits {

            @Config.Name("ItemConduit ItemStack Cache")
            public boolean cachedItemConduit = true;

        }

        public static class FluxNetworks {

            @Config.Name("TheOneProbe Integration")
            public boolean fixTop = true;

        }

        public static class IndustrialCraft2 {

            @Config.Name("GradualRecipe Fixes")
            public boolean gradualRecipe = true;

        }

        public static class InGameInfoXML {

            @Config.Name("PlayerHandler Fixes")
            public boolean playerHandler = true;

        }

        public static class ImmersiveEngineering {

            @Config.Name("Multiblock Structure Container Fixes")
            public boolean blockIEMultiblock = true;

            @Config.Name("JerryCan Fixes")
            public boolean fixJerryCanRecipe = true;

        }

        public static class MoreElectricTools {

            @Config.Name("LifeSupports Fixes")
            public boolean fixLifeSupports = true;

        }

        public static class ScalingGuis {

            @Config.Name("JsonHelper Crash Fixes")
            public boolean fixJsonHelper = true;

        }

        public static class Techguns {

            @Config.Name("TGPermissions Crash Fixes")
            public boolean tgPermissions = true;

            @Config.Name("Invalid Recipe Fixes")
            public boolean fixAmmoSumRecipeFactory = true;

        }

        public static class TheOneProbe {

            @Config.Name("Player Entity Render Fixes")
            public boolean fixRenderHelper = true;

        }

        public static class ThermalDynamics {

            @Config.Name("Fluid Duplicate Fixes")
            public boolean fixFluidDuplicate = true;

        }

    }

    public static class Performance {

        @Config.Name("Vanilla")
        public final Vanilla vanilla = new Vanilla();

        @Config.Name("Avaritia")
        public final Avaritia avaritia = new Avaritia();

        @Config.Name("Biomes O' Plenty")
        public final BiomesOPlenty biomesOPlenty = new BiomesOPlenty();

        @Config.Name("Extra Botany")
        public final ExtraBotany extraBotany = new ExtraBotany();

        @Config.Name("Blood Magic")
        public final BloodMagic bloodMagic = new BloodMagic();

        @Config.Name("Botania")
        public final Botania botania = new Botania();

        @Config.Name("Chisel")
        public final Chisel chisel = new Chisel();

        @Config.Name("Custom Loading Screen")
        public final CustomLoadingScreen customLoadingScreen = new CustomLoadingScreen();

        @Config.Name("EnderIO")
        public final EnderIO enderIO = new EnderIO();

        @Config.Name("EnderIO Conduits")
        public final EnderIOConduits enderIOConduits = new EnderIOConduits();

        @Config.Name("IndustrialCraft2")
        public final IndustrialCraft2 industrialCraft2 = new IndustrialCraft2();

        @Config.Name("InGameInfoXML")
        public final InGameInfoXML inGameInfoXML = new InGameInfoXML();

        @Config.Name("Mekanism")
        public final Mekanism mekanism = new Mekanism();

        public static class Vanilla {

            @Config.Name("Captured Block Snapshots Improvements")
            public boolean capturedBlockSnapshots = true;

            @Config.Name("Captured Block Snapshots Improvements - OreExcavation Integration")
            public boolean capturedBlockSnapshotsMiningAgentIntegration = true;

            @Config.Name("Chunk TileEntityMap Improvements")
            public boolean blockPos2ValueMap = true;

        }

        public static class Avaritia {

            @Config.Name("TileBase Improvements")
            public boolean tileBase = true;

        }

        public static class BiomesOPlenty {

            @Config.Name("TrailManager Async")
            public boolean trailManager = true;

        }

        public static class ExtraBotany {

            @Config.Name("PersistentVariableHandler Async")
            public boolean persistentVariableHandler = true;

        }

        public static class BloodMagic {

            @Config.Name("BloodAltar Improvements")
            public boolean bloodAltar = true;

        }

        public static class Botania {

            @Config.Name("Spark Entity Improvements")
            public boolean sparkImprovements = true;

            @Config.RangeInt(min = 2, max = 60)
            @Config.Name("Spark Max Work Delay")
            public int sparkMaxWorkDelay = 10;

        }

        public static class Chisel {

            @Config.Name("Auto Chisel Improvements")
            public boolean autoChiselImprovements = true;

            @Config.RangeInt(min = 20, max = 100)
            @Config.Name("Auto Chisel Max Work Delay")
            public int autoChiselMaxWorkDelay = 100;

        }

        public static class CustomLoadingScreen {

            @Config.Name("Texture Cleanup")
            public boolean splashProgress = true;

        }

        public static class EnderIO {

            @Config.Name("ItemTools Improvements")
            public boolean itemTools = true;

            @Config.Name("TileEntityBase Improvements")
            public boolean tileEntityBase = true;

        }

        public static class EnderIOConduits {

            @Config.Name("AbstractConduit Improvements")
            public boolean abstractConduit = true;

        }

        public static class IndustrialCraft2 {

            @Config.Name("EnergyCalculatorLeg Improvements")
            public boolean energyCalculatorLeg = true;

            @Config.Name("ItemUpgradeModule Improvements")
            public boolean itemUpgradeModule = false;

            @Config.Name("ItemUpgradeModule Work Delay")
            public int itemUpgradeModuleWorkDelay = 5;

        }

        public static class InGameInfoXML {

            @Config.Name("HUD Framebuffer")
            public boolean hudFrameBuffer = true;

            @Config.Name("HUD FPS")
            @Config.RangeInt(min = 5, max = 60)
            public int hudFrameRate = 10;

        }

        public static class Mekanism {

            @Config.Name("PipeUtils Improvements")
            public boolean pipeUtils = true;

        }

    }

    public static class Features {
        @Config.Name("Enable Custom Game Title")
        public final boolean enbleTitle = false;

        @Config.Name("Title Use Hitokoto API")
        public final boolean hitokoto = true;

        @Config.Name("Custom Game Title")
        public final String title = "Minecraft 1.12.2";

        @Config.Name("Font Scale")
        public final FontScale fontScale = new FontScale();

        @Config.Name("Astral Sorcery")
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

        @Config.Name("RGB Chat")
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

            @Config.Name("Enable Message Compat")
            public boolean messageCompat = false;

        }

        public static class Botania {

            @Config.Name("Disable Cosmetic Recipe")
            public boolean disableCosmeticRecipe = false;

        }

        public static class LegendaryTooltips {

            @Config.Name("Disable Title Wrap")
            public boolean tooltipDecor = false;

        }

        public static class Mekanism {

            @Config.Name("TOP Support")
            public boolean topSupport = true;

            @Config.Name("FluxNetworks Support")
            public boolean fluxNetworksSupport = true;

        }

        public static class NuclearCraftOverhauled {

            @Config.Name("Disable RadiationCapability")
            public boolean removeRadiationCapabilityHandler = false;

        }

        public static class RGBChat {

            @Config.Name("TrueRGBSimpleRenderer Improvements")
            public boolean cachedRGBFontRenderer = true;

        }

        public static class Techguns {

            @Config.Name("Force Security Mode")
            public boolean forceSecurityMode = true;

            @Config.Name("Bullet is Projectile")
            public boolean tgDamageSource = true;

        }

        public static class MoreElectricTools {

            @Config.Name("Remove EfficientEnergyCost Enchantment")
            public boolean disableEfficientEnergyCost = false;

        }

    }

}
