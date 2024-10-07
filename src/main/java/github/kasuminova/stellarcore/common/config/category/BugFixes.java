package github.kasuminova.stellarcore.common.config.category;

import net.minecraftforge.common.config.Config;

public class BugFixes {

    @Config.Name("Vanilla")
    public final Vanilla vanilla = new Vanilla();

    @Config.Name("Critical")
    public final Critical critical = new Critical();

    @Config.Name("Container")
    public final Container container = new Container();

    @Config.Name("AdvancedRocketry")
    public final AdvancedRocketry advancedRocketry = new AdvancedRocketry();

    @Config.Name("AncientSpellCraft")
    public final AncientSpellCraft ancientSpellCraft = new AncientSpellCraft();

    @Config.Name("ArmourersWorkshop")
    public final ArmourersWorkshop armourersWorkshop = new ArmourersWorkshop();

    @Config.Name("AstralSorcery")
    public final AstralSorcery astralSorcery = new AstralSorcery();

    @Config.Name("Avaritaddons")
    public final Avaritaddons avaritaddons = new Avaritaddons();

    @Config.Name("Botania")
    public final Botania botania = new Botania();

    @Config.Name("CoFHCore")
    public final CoFHCore coFHCore = new CoFHCore();

    @Config.Name("CustomStartingGear")
    public final CustomStartingGear customStartingGear = new CustomStartingGear();

    @Config.Name("DraconicEvolution")
    public final DraconicEvolution draconicEvolution = new DraconicEvolution();

    @Config.Name("EnderIOConduits")
    public final EnderIOConduits enderIOConduits = new EnderIOConduits();

    @Config.Name("EnderUtilities")
    public final EnderUtilities enderUtilities = new EnderUtilities();

    @Config.Name("ExtraBotany")
    public final ExtraBotany extraBotany = new ExtraBotany();

    @Config.Name("FluxNetworks")
    public final FluxNetworks fluxNetworks = new FluxNetworks();

    @Config.Name("IndustrialCraft2")
    public final IndustrialCraft2 industrialCraft2 = new IndustrialCraft2();

    @Config.Name("InGameInfoXML")
    public final InGameInfoXML inGameInfoXML = new InGameInfoXML();

    @Config.Name("ImmersiveEngineering")
    public final ImmersiveEngineering immersiveEngineering = new ImmersiveEngineering();

    @Config.Name("JourneyMap")
    public final JourneyMap journeyMap = new JourneyMap();

    @Config.Name("LibVulpes")
    public final LibVulpes libVulpes = new LibVulpes();

    @Config.Name("Mekanism")
    public final Mekanism mekanism = new Mekanism();

    @Config.Name("ModularRouters")
    public final ModularRouters modularRouters = new ModularRouters();

    @Config.Name("MoreElectricTools")
    public final MoreElectricTools moreElectricTools = new MoreElectricTools();

    @Config.Name("MrCrayfishFurniture")
    public final MrCrayfishFurniture mrCrayfishFurniture = new MrCrayfishFurniture();

    @Config.Name("RPSIdeas")
    public final RPSIdeas rpsIdeas = new RPSIdeas();

    @Config.Name("ScalingGuis")
    public final ScalingGuis scalingGuis = new ScalingGuis();

    @Config.Name("Sync")
    public final Sync sync = new Sync();

    @Config.Name("TConEvo")
    public final TConEvo tConEvo = new TConEvo();

    @Config.Name("Techguns")
    public final Techguns techguns = new Techguns();

    @Config.Name("TheOneProbe")
    public final TheOneProbe theOneProbe = new TheOneProbe();

    @Config.Name("ThermalDynamics")
    public final ThermalDynamics thermalDynamics = new ThermalDynamics();

    @Config.Name("ThermalExpansion")
    public final ThermalExpansion thermalExpansion = new ThermalExpansion();

    public static class Vanilla {

        @Config.Comment({
                "Unlocks the size limit of NBT and removes the length limit of NBTTagCompound and NBTTagList,",
                "usually many mods will have this feature, you just need to enable one of these modules."
        })
        @Config.RequiresMcRestart
        @Config.Name("LongNBTKiller")
        public boolean longNBTKiller = false;

        @Config.Comment({
                "The maximum depth of NBTTagCompound and NBTTagList.",
                "It will only take effect if LongNBTKiller is enabled."
        })
        @Config.Name("MaxNBTDepth")
        public int maxNBTDepth = 2048;

        @Config.Comment({
                "The maximum size of NBT.",
                "It will only take effect if LongNBTKiller is enabled."
        })
        @Config.Name("MaxNBTSize")
        public int maxNBTSize = 1024 * 1024 * 2 * 8;

        @Config.Comment({
                "If the NBT size is larger than the maximum size, it will display a warning message.",
                "It will only take effect if LongNBTKiller is enabled."
        })
        @Config.Name("DisplayLargeNBTWarning")
        public boolean displayLargeNBTWarning = true;

    }

    public static class Critical {

        @Config.Comment("Usually just set it to true, this option fixed a serious network packet problem.")
        @Config.Name("GuGuUtilsSetContainerPacket")
        public boolean guguUtilsSetContainerPacket = true;

    }

    public static class Container {

        @Config.Comment({
                "A generic feature that when a player's open TileEntity GUI is uninstalled, ",
                "it also forces the player's open GUI to be closed."
        })
        @Config.Name("ContainerUnloadTileEntityFixes")
        public boolean containerTileEntityFixes = false;

    }

    public static class AdvancedRocketry {

        @Config.Comment("Fix the NPE problem that occasionally occurs with BiomeChanger.")
        @Config.Name("ItemBiomeChanger")
        public boolean itemBiomeChanger = true;

        @Config.Comment({
                "When the planetDefs.xml file is corrupted, make it regenerate the file instead of letting it damn near crash.",
                "This is usually only a problem if the game process is unexpectedly exited, and the file is usually unrecoverable without a backup."
        })
        @Config.Name("PreventDimensionManagerCrash")
        public boolean dimensionManager = true;

    }

    public static class AncientSpellCraft {

        @Config.Comment({
                "(Client Only) Fix a memory leak caused by AncientSpellCraft's FakePlayer,",
                "mainly in that it would cause the first world loaded not to be cleaned up by Java GC.",
                "Experimental, if a crash occurs with anything related to ASFakePlayer, please report this issue immediately."
        })
        @Config.Name("ASFakePlayerFixes")
        public boolean asFakePlayer = false;

    }

    public static class ArmourersWorkshop {

        @Config.Comment("Cleanroom only, used to fix an issue that caused the game to crash when unloading skin texture files.")
        @Config.Name("SkinTextureCrashFixes")
        public boolean skinTexture = true;

    }

    public static class AstralSorcery {

        @Config.Comment("This option is used to fix occasional crashes related to PlayerAttributeMap.")
        @Config.RequiresWorldRestart
        @Config.Name("PlayerAttributeMapCrashFixes")
        public boolean playerAttributeMap = true;

        @Config.Comment("This option is used to fix some item duplication issues on Astral Tome's constellation paper collection page.")
        @Config.Name("ContainerJournalFixes")
        public boolean containerJournal = true;

    }

    public static class Avaritaddons {

        @Config.Comment("This option is used to fix some item duplication issues with Auto Extreme Crafting Table.")
        @Config.Name("TileEntityExtremeAutoCrafterFixes")
        public boolean tileEntityExtremeAutoCrafter = true;

    }

    public static class Botania {

        @Config.Comment("(Client Only) Automatically clean up data when the player switches worlds, optional feature as WeakHashMap does not usually cause memory leaks.")
        @Config.Name("AutoCleanManaNetworkHandler")
        public boolean manaNetworkHandler = true;

    }

    public static class CoFHCore {

        @Config.Comment("This option is used to fix some item duplication issues with any containers related to TE5.")
        @Config.Name("ContainerInventoryItemFixes")
        public boolean containerInventoryItem = true;

        @Config.Comment({
                "This option is used to fix an issue that would accidentally cause non-stackable items to",
                " exceed their maximum number of stacks."
        })
        @Config.Name("TileInventoryFixes")
        public boolean tileInventory = true;

    }

    public static class CustomStartingGear {

        @Config.Comment({
                "This option causes CustomStartingGear to standardize the encoding of file reads to UTF-8,",
                "preventing them from having problems on computers in certain regions."
        })
        @Config.Name("DataManagerCharSetFixes")
        public boolean dataManager = true;

    }

    public static class DraconicEvolution {

        @Config.Comment("This option is used to fix some item duplication issues with CraftingInjector.")
        @Config.Name("CraftingInjectorFixes")
        public boolean craftingInjector = true;

    }

    public static class EnderIOConduits {

        @Config.Comment({
                "A somewhat disruptive feature fix that modifies some of the way item conduits work,",
                "allowing some special cases to store extracted items inside the conduit,",
                "which will help fix some item duplication issues,",
                "but may introduce a slight performance overhead and some unexpected filter issues."
        })
        @Config.Name("ItemConduitItemStackCache")
        public boolean cachedItemConduit = false;

    }

    public static class EnderUtilities {

        @Config.Comment({
                "Fix an issue where HandyBag sometimes picking up items would cause them to be duplicated,",
                "with the side effect that the player's item bar would no longer be populated when picking up matching items."
        })
        @Config.Name("ItemHandyBagDupeFixes")
        public boolean itemHandyBag = true;

        @Config.Comment({
                "Fix an issue where Nullifier sometimes picking up items would cause them to be duplicated,",
                "with the side effect that the player's item bar would no longer be populated when picking up matching items."
        })
        @Config.Name("ItemNullifierDupeFixes")
        public boolean itemNullifier = true;

    }

    public static class ExtraBotany {

        @Config.Comment("Prevents the Mana Liquefaction Device from storing far more liquid magic than it is set to store.")
        @Config.Name("TileManaLiquefactionFixes")
        public boolean tileManaLiquefaction = true;

    }

    public static class FluxNetworks {

        @Config.Comment("Fixes an issue where TheOneProbe on a dedicated server shows localized text anomalies.")
        @Config.Name("TheOneProbeIntegration")
        public boolean fixTop = true;

        @Config.Comment("Possible fix for duplicate users or even crashes on player networks in some cases.")
        @Config.Name("SynchronizeFixes")
        public boolean synchronize = true;

    }

    public static class IndustrialCraft2 {

        @Config.Comment("Fixed an issue where some item repair recipes would duplication items.")
        @Config.Name("GradualRecipeFixes")
        public boolean gradualRecipe = true;

        @Config.Comment("Fixed an issue where the orientation determined by Ejector / Pulling Upgrade was the opposite of what it actually was.")
        @Config.Name("StackUtilInvFacingFixes")
        public boolean stackUtilInvFacingFixes = true;

    }

    public static class InGameInfoXML {

        @Config.Comment("Fix the issue where a paragraph would pop up to report an error in a server environment, that's all.")
        @Config.Name("PlayerHandlerFixes")
        public boolean playerHandler = true;

    }

    public static class ImmersiveEngineering {

        @Config.Comment("Fixes an issue that would cause items to duplicate in certain special cases, although they were a bit tricky to reproduce.")
        @Config.Name("MultiblockStructureContainerFixes")
        public boolean blockIEMultiblock = true;

        @Config.Comment("Fixes an issue that would cause fluids to duplicate in some special cases, although they were a bit tricky to reproduce.")
        @Config.Name("JerryCanFixes")
        public boolean fixJerryCanRecipe = true;

        @Config.Comment("Makes Excavator not drop twice drops when digging blocks (possible side effect).")
        @Config.Name("TileEntityExcavatorDigBlockFixes")
        public boolean tileEntityExcavator = true;

        @Config.Comment({
                "Fixes an issue that caused ArcFurnace's item bar items to stack",
                "more than their items themselves under certain special circumstances, helping to fix item duplication."
        })
        @Config.Name("TileEntityArcFurnaceInventoryFixes")
        public boolean tileEntityArcFurnace = true;

        @Config.Comment({
                "(Client Only) Clear the model cache when the player switches worlds to avoid memory leaks.",
                "Minor performance impact. Mainly a problem when installing with other mods."
        })
        @Config.Name("AutoCleanRenderCache")
        public boolean renderCache = true;

        @Config.Comment({
                "Immediately exit the thread when `Immersive Engineering Contributors Thread` encounters an error while reading JSON,",
                "instead of always printing the error."
        })
        @Config.Name("IEContributorsThreadExceptionFixes")
        public boolean contributorSpecialsDownloader = true;

    }

    public static class JourneyMap {

        @Config.Comment("(Client Only) Automatically clears the radar player cache when a player switches worlds to avoid memory leaks caused in the client.")
        @Config.Name("AutoCleanPlayerRadar")
        public boolean playerRadar = true;

    }

    public static class LibVulpes {

        @Config.Comment({
                "(Client Only) Automatically clean up InputSyncHandler's spaceDown data when the player switches worlds.",
                "Although libvulpes has already coded this judgment, there is still a small chance that it will trigger a memory leak."
        })
        @Config.Name("AutoCleanInputSyncHandlerData")
        public boolean inputSyncHandler = true;

    }

    public static class Mekanism {

        @Config.Comment({
                "(Client Only) Automatically clean up old player data when the player switches worlds to address some memory leaks,",
                "and while Mekanism has written cleanup features, they will only clean up when returning to the main menu."
        })
        @Config.Name("AutoCleanPortableTeleports")
        public boolean portableTeleports = true;

    }

    public static class ModularRouters {

        @Config.Comment("Prevent routers from recognizing fluid bucket containers to avoid unexpected fluid replication problems.")
        @Config.Name("BufferHandlerFluidHandlerFixes")
        public boolean bufferHandler = true;

    }

    public static class MoreElectricTools {

        @Config.Comment("Items such as Electric First Aid Life Support do not continue to work if the player has died.")
        @Config.Name("LifeSupportsFixes")
        public boolean fixLifeSupports = true;

    }

    public static class MrCrayfishFurniture {

        @Config.Comment("Stops the game from freezing in certain special cases, mainly occurring on photo frames and other similar blocks.")
        @Config.RequiresMcRestart
        @Config.Name("ImageCacheCrashFixes")
        public boolean imageCache = true;

        @Config.Comment("Make blocks be rotated without losing their internal attributes and items (possibly not all blocks).")
        @Config.Name("RotatableFurniture")
        public boolean rotatableFurniture = false;

        @Config.Comment("Problem preventing certain container items from duplicate.")
        @Config.Name("BlockFurnitureTileFixes")
        public boolean blockFurnitureTile = true;

        @Config.Comment("Stopping washing machines from repairing non-repairable items.")
        @Config.Name("WashingMachineDamageFixes")
        public boolean washingMachine = true;

    }

    public static class RPSIdeas {

        @Config.Comment("(Client Only) Fix memory leaks caused by improper object management on the client side.")
        @Config.Name("ItemBioticSensorMemoryLeakFixes")
        public boolean itemBioticSensor = true;

    }

    public static class ScalingGuis {

        @Config.Comment("Fixes an issue that caused a crash when deleting invalid GUI configurations.")
        @Config.Name("JsonHelperCrashFixes")
        public boolean fixJsonHelper = true;

    }

    public static class Sync {

        @Config.Comment("A special fix that keeps Sync from triggering some weird item duplication issues when installed with Techguns.")
        @Config.Name("TechgunsDuplicationFixes")
        public boolean techgunsDuplicationFixes = true;

        @Config.Comment("Make players get off their mounts when they die.")
        @Config.Name("RidingFixes")
        public boolean ridingFixes = true;

    }

    public static class TConEvo {

        @Config.Comment({
                "Fix a special crash issue that would cause special cases,",
                "reporting that they were attributed to xyz.phanta.tconevo.integration.avaritia.client.AvaritiaMaterialModel$BakedAvaritiaMaterialModel$ WithoutHalo handleCosmicLighting()."
        })
        @Config.Name("HandleCosmicLightingNPEFixes")
        public boolean handleCosmicLightingNPEFixes = true;

    }

    public static class Techguns {

        @Config.Comment("Fixes an issue that would cause crashes in server environments.")
        @Config.Name("TGPermissionsCrashFixes")
        public boolean tgPermissions = true;

        @Config.Comment("Fix for recipes not working properly for certain items (Techguns only).")
        @Config.Name("InvalidRecipeFixes")
        public boolean fixAmmoSumRecipeFactory = true;

        @Config.Comment("Fixes an issue that would cause crashes in server environments.")
        @Config.Name("ServerSideEntityCrashFixes")
        public boolean serverSideEntityCrashFixes = true;

    }

    public static class TheOneProbe {

        @Config.Comment("Prevents TheOneProbe from rendering the entity/player in such a way that their head is locked to a fixed pitch.")
        @Config.Name("PlayerEntityRenderFixes")
        public boolean fixRenderHelper = true;

    }

    public static class ThermalDynamics {

        @Config.Comment("Fix a fluid duplication issue where they would only appear on Super-Laminar FluidDuct.")
        @Config.Name("FluidDuplicateFixes")
        public boolean fixFluidDuplicate = true;

    }

    public static class ThermalExpansion {

        @Config.Comment("The problem with stopping a backpack from replicating is that this probably does the same thing as UniversalTweaks.")
        @Config.Name("ContainerSatchelFilterFixes")
        public boolean containerSatchelFilter = true;

    }

}
