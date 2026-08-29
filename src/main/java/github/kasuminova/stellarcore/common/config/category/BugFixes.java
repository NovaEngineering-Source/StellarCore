package github.kasuminova.stellarcore.common.config.category;

import net.minecraftforge.common.config.Config;

public class BugFixes {

    @Config.LangKey("stellar_core.config.bugfixes.vanilla")
    @Config.Name("Vanilla")
    public final Vanilla vanilla = new Vanilla();

    @Config.LangKey("stellar_core.config.bugfixes.critical")
    @Config.Name("Critical")
    public final Critical critical = new Critical();

    @Config.LangKey("stellar_core.config.bugfixes.container")
    @Config.Name("Container")
    public final Container container = new Container();

    @Config.LangKey("stellar_core.config.bugfixes.advancedRocketry")
    @Config.Name("AdvancedRocketry")
    public final AdvancedRocketry advancedRocketry = new AdvancedRocketry();

    @Config.LangKey("stellar_core.config.bugfixes.armourersWorkshop")
    @Config.Name("ArmourersWorkshop")
    public final ArmourersWorkshop armourersWorkshop = new ArmourersWorkshop();

    @Config.LangKey("stellar_core.config.bugfixes.astralSorcery")
    @Config.Name("AstralSorcery")
    public final AstralSorcery astralSorcery = new AstralSorcery();

    @Config.LangKey("stellar_core.config.bugfixes.avaritaddons")
    @Config.Name("Avaritaddons")
    public final Avaritaddons avaritaddons = new Avaritaddons();

    @Config.LangKey("stellar_core.config.bugfixes.botania")
    @Config.Name("Botania")
    public final Botania botania = new Botania();

    @Config.LangKey("stellar_core.config.bugfixes.coFHCore")
    @Config.Name("CoFHCore")
    public final CoFHCore coFHCore = new CoFHCore();

    @Config.LangKey("stellar_core.config.bugfixes.customStartingGear")
    @Config.Name("CustomStartingGear")
    public final CustomStartingGear customStartingGear = new CustomStartingGear();

    @Config.LangKey("stellar_core.config.bugfixes.draconicEvolution")
    @Config.Name("DraconicEvolution")
    public final DraconicEvolution draconicEvolution = new DraconicEvolution();

    @Config.LangKey("stellar_core.config.bugfixes.ebWizardry")
    @Config.Name("EBWizardry")
    public final EBWizardry ebWizardry = new EBWizardry();

    @Config.LangKey("stellar_core.config.bugfixes.enderIOConduits")
    @Config.Name("EnderIOConduits")
    public final EnderIOConduits enderIOConduits = new EnderIOConduits();

    @Config.LangKey("stellar_core.config.bugfixes.enderUtilities")
    @Config.Name("EnderUtilities")
    public final EnderUtilities enderUtilities = new EnderUtilities();

    @Config.LangKey("stellar_core.config.bugfixes.extraBotany")
    @Config.Name("ExtraBotany")
    public final ExtraBotany extraBotany = new ExtraBotany();

    @Config.LangKey("stellar_core.config.bugfixes.fluxNetworks")
    @Config.Name("FluxNetworks")
    public final FluxNetworks fluxNetworks = new FluxNetworks();

    @Config.LangKey("stellar_core.config.bugfixes.industrialCraft2")
    @Config.Name("IndustrialCraft2")
    public final IndustrialCraft2 industrialCraft2 = new IndustrialCraft2();

    @Config.LangKey("stellar_core.config.bugfixes.inGameInfoXML")
    @Config.Name("InGameInfoXML")
    public final InGameInfoXML inGameInfoXML = new InGameInfoXML();

    @Config.LangKey("stellar_core.config.bugfixes.immersiveEngineering")
    @Config.Name("ImmersiveEngineering")
    public final ImmersiveEngineering immersiveEngineering = new ImmersiveEngineering();

    @Config.LangKey("stellar_core.config.bugfixes.journeyMap")
    @Config.Name("JourneyMap")
    public final JourneyMap journeyMap = new JourneyMap();

    @Config.LangKey("stellar_core.config.bugfixes.libVulpes")
    @Config.Name("LibVulpes")
    public final LibVulpes libVulpes = new LibVulpes();

    @Config.LangKey("stellar_core.config.bugfixes.mekanism")
    @Config.Name("Mekanism")
    public final Mekanism mekanism = new Mekanism();

    @Config.LangKey("stellar_core.config.bugfixes.modularRouters")
    @Config.Name("ModularRouters")
    public final ModularRouters modularRouters = new ModularRouters();

    @Config.LangKey("stellar_core.config.bugfixes.moreElectricTools")
    @Config.Name("MoreElectricTools")
    public final MoreElectricTools moreElectricTools = new MoreElectricTools();

    @Config.LangKey("stellar_core.config.bugfixes.mrCrayfishFurniture")
    @Config.Name("MrCrayfishFurniture")
    public final MrCrayfishFurniture mrCrayfishFurniture = new MrCrayfishFurniture();

    @Config.LangKey("stellar_core.config.bugfixes.rpsIdeas")
    @Config.Name("RPSIdeas")
    public final RPSIdeas rpsIdeas = new RPSIdeas();

    @Config.LangKey("stellar_core.config.bugfixes.scalingGuis")
    @Config.Name("ScalingGuis")
    public final ScalingGuis scalingGuis = new ScalingGuis();

    @Config.LangKey("stellar_core.config.bugfixes.sync")
    @Config.Name("Sync")
    public final Sync sync = new Sync();

    @Config.LangKey("stellar_core.config.bugfixes.tConEvo")
    @Config.Name("TConEvo")
    public final TConEvo tConEvo = new TConEvo();

    @Config.LangKey("stellar_core.config.bugfixes.techguns")
    @Config.Name("Techguns")
    public final Techguns techguns = new Techguns();

    @Config.LangKey("stellar_core.config.bugfixes.theOneProbe")
    @Config.Name("TheOneProbe")
    public final TheOneProbe theOneProbe = new TheOneProbe();

    @Config.LangKey("stellar_core.config.bugfixes.thermalDynamics")
    @Config.Name("ThermalDynamics")
    public final ThermalDynamics thermalDynamics = new ThermalDynamics();

    @Config.LangKey("stellar_core.config.bugfixes.thermalExpansion")
    @Config.Name("ThermalExpansion")
    public final ThermalExpansion thermalExpansion = new ThermalExpansion();

    public static class Vanilla {

        @Config.Comment({
                "Unlocks the size limit of NBT and removes the length limit of NBTTagCompound and NBTTagList,",
                "usually many mods will have this feature, you just need to enable one of these modules."
        })
        @Config.RequiresMcRestart
        @Config.LangKey("stellar_core.config.bugfixes.vanilla.longNBTKiller")
        @Config.Name("LongNBTKiller")
        public boolean longNBTKiller = false;

        @Config.Comment({
                "The maximum depth of NBTTagCompound and NBTTagList.",
                "It will only take effect if LongNBTKiller is enabled."
        })
        @Config.LangKey("stellar_core.config.bugfixes.vanilla.maxNBTDepth")
        @Config.Name("MaxNBTDepth")
        public int maxNBTDepth = 2048;

        @Config.Comment({
                "The maximum size of NBT.",
                "It will only take effect if LongNBTKiller is enabled."
        })
        @Config.LangKey("stellar_core.config.bugfixes.vanilla.maxNBTSize")
        @Config.Name("MaxNBTSize")
        public int maxNBTSize = 1024 * 1024 * 2 * 8;

        @Config.Comment({
                "If the NBT size is larger than the maximum size, it will display a warning message.",
                "It will only take effect if LongNBTKiller is enabled."
        })
        @Config.LangKey("stellar_core.config.bugfixes.vanilla.displayLargeNBTWarning")
        @Config.Name("DisplayLargeNBTWarning")
        public boolean displayLargeNBTWarning = true;

        @Config.Comment("Fix the NPE problem that occasionally occurs with the client when the server sends a null block packet.")
        @Config.LangKey("stellar_core.config.bugfixes.vanilla.clientNullBlockPacket")
        @Config.Name("ClientNullBlockPacket")
        public boolean clientNullBlockPacket = true;

    }

    public static class Critical {

        @Config.Comment("Usually just set it to true, this option fixed a serious network packet problem.")
        @Config.LangKey("stellar_core.config.bugfixes.critical.guGuUtilsSetContainerPacket")
        @Config.Name("GuGuUtilsSetContainerPacket")
        public boolean guguUtilsSetContainerPacket = true;

    }

    public static class Container {

        @Config.Comment({
                "A generic feature that when a player's open TileEntity GUI is uninstalled, ",
                "it also forces the player's open GUI to be closed."
        })
        @Config.LangKey("stellar_core.config.bugfixes.container.containerUnloadTileEntityFixes")
        @Config.Name("ContainerUnloadTileEntityFixes")
        public boolean containerTileEntityFixes = false;

        @Config.Comment("Restricts the player from interacting with the world's blocks when the player opens any container interface (except the player inventory).")
        @Config.LangKey("stellar_core.config.bugfixes.container.containerInteractRestriction")
        @Config.Name("ContainerInteractRestriction")
        public boolean containerInteract = false;

    }

    public static class AdvancedRocketry {

        @Config.Comment("Fix the NPE problem that occasionally occurs with BiomeChanger.")
        @Config.LangKey("stellar_core.config.bugfixes.advancedRocketry.itemBiomeChanger")
        @Config.Name("ItemBiomeChanger")
        public boolean itemBiomeChanger = true;

        @Config.Comment({
                "When the planetDefs.xml file is corrupted, make it regenerate the file instead of letting it damn near crash.",
                "This is usually only a problem if the game process is unexpectedly exited, and the file is usually unrecoverable without a backup."
        })
        @Config.LangKey("stellar_core.config.bugfixes.advancedRocketry.preventDimensionManagerCrash")
        @Config.Name("PreventDimensionManagerCrash")
        public boolean dimensionManager = true;

    }

    public static class ArmourersWorkshop {

        @Config.Comment("Cleanroom only, used to fix an issue that caused the game to crash when unloading skin texture files.")
        @Config.LangKey("stellar_core.config.bugfixes.armourersWorkshop.skinTextureCrashFixes")
        @Config.Name("SkinTextureCrashFixes")
        public boolean skinTexture = true;

    }

    public static class AstralSorcery {

        @Config.Comment("This option is used to fix occasional crashes related to PlayerAttributeMap.")
        @Config.RequiresWorldRestart
        @Config.LangKey("stellar_core.config.bugfixes.astralSorcery.playerAttributeMapCrashFixes")
        @Config.Name("PlayerAttributeMapCrashFixes")
        public boolean playerAttributeMap = true;

        @Config.Comment("This option is used to fix some item duplication issues on Astral Tome's constellation paper collection page.")
        @Config.LangKey("stellar_core.config.bugfixes.astralSorcery.containerJournalFixes")
        @Config.Name("ContainerJournalFixes")
        public boolean containerJournal = true;

    }

    public static class Avaritaddons {

        @Config.Comment("This option is used to fix some item duplication issues with Auto Extreme Crafting Table.")
        @Config.LangKey("stellar_core.config.bugfixes.avaritaddons.tileEntityExtremeAutoCrafterFixes")
        @Config.Name("TileEntityExtremeAutoCrafterFixes")
        public boolean tileEntityExtremeAutoCrafter = true;

    }

    public static class Botania {

        @Config.Comment("(Client Only) Automatically clean up data when the player switches worlds, optional feature as WeakHashMap does not usually cause memory leaks.")
        @Config.LangKey("stellar_core.config.bugfixes.botania.autoCleanManaNetworkHandler")
        @Config.Name("AutoCleanManaNetworkHandler")
        public boolean manaNetworkHandler = true;

    }

    public static class CoFHCore {

        @Config.Comment("This option is used to fix some item duplication issues with any containers related to TE5.")
        @Config.LangKey("stellar_core.config.bugfixes.coFHCore.containerInventoryItemFixes")
        @Config.Name("ContainerInventoryItemFixes")
        public boolean containerInventoryItem = true;

        @Config.Comment({
                "This option is used to fix an issue that would accidentally cause non-stackable items to",
                " exceed their maximum number of stacks."
        })
        @Config.LangKey("stellar_core.config.bugfixes.coFHCore.tileInventoryFixes")
        @Config.Name("TileInventoryFixes")
        public boolean tileInventory = true;

    }

    public static class CustomStartingGear {

        @Config.Comment({
                "This option causes CustomStartingGear to standardize the encoding of file reads to UTF-8,",
                "preventing them from having problems on computers in certain regions."
        })
        @Config.LangKey("stellar_core.config.bugfixes.customStartingGear.dataManagerCharSetFixes")
        @Config.Name("DataManagerCharSetFixes")
        public boolean dataManager = true;

    }

    public static class DraconicEvolution {

        @Config.Comment("This option is used to fix some item duplication issues with CraftingInjector.")
        @Config.LangKey("stellar_core.config.bugfixes.draconicEvolution.craftingInjectorFixes")
        @Config.Name("CraftingInjectorFixes")
        public boolean craftingInjector = true;

    }
    
    public static class EBWizardry {

        @Config.Comment("Fix an issue where Imbuement Altar could copy items using a special interact method.")
        @Config.LangKey("stellar_core.config.bugfixes.ebWizardry.blockImbuementAltarDupeFixes")
        @Config.Name("BlockImbuementAltarDupeFixes")
        public boolean blockImbuementAltar = true;

    }

    public static class EnderIOConduits {

        @Config.Comment({
                "A somewhat disruptive feature fix that modifies some of the way item conduits work,",
                "allowing some special cases to store extracted items inside the conduit,",
                "which will help fix some item duplication issues,",
                "but may introduce a slight performance overhead and some unexpected filter issues."
        })
        @Config.LangKey("stellar_core.config.bugfixes.enderIOConduits.itemConduitItemStackCache")
        @Config.Name("ItemConduitItemStackCache")
        public boolean cachedItemConduit = false;

    }

    public static class EnderUtilities {

        @Config.Comment({
                "Fix an issue where HandyBag sometimes picking up items would cause them to be duplicated,",
                "with the side effect that the player's item bar would no longer be populated when picking up matching items."
        })
        @Config.LangKey("stellar_core.config.bugfixes.enderUtilities.itemHandyBagDupeFixes")
        @Config.Name("ItemHandyBagDupeFixes")
        public boolean itemHandyBag = true;

        @Config.Comment({
                "Fix an issue where Nullifier sometimes picking up items would cause them to be duplicated,",
                "with the side effect that the player's item bar would no longer be populated when picking up matching items."
        })
        @Config.LangKey("stellar_core.config.bugfixes.enderUtilities.itemNullifierDupeFixes")
        @Config.Name("ItemNullifierDupeFixes")
        public boolean itemNullifier = true;

    }

    public static class ExtraBotany {

        @Config.Comment("Prevents the Mana Liquefaction Device from storing far more liquid magic than it is set to store.")
        @Config.LangKey("stellar_core.config.bugfixes.extraBotany.tileManaLiquefactionFixes")
        @Config.Name("TileManaLiquefactionFixes")
        public boolean tileManaLiquefaction = true;

    }

    public static class FluxNetworks {

        @Config.Comment("Fixes an issue where TheOneProbe on a dedicated server shows localized text anomalies.")
        @Config.LangKey("stellar_core.config.bugfixes.fluxNetworks.theOneProbeIntegration")
        @Config.Name("TheOneProbeIntegration")
        public boolean fixTop = true;

        @Config.Comment("Possible fix for duplicate users or even crashes on player networks in some cases.")
        @Config.LangKey("stellar_core.config.bugfixes.fluxNetworks.synchronizeFixes")
        @Config.Name("SynchronizeFixes")
        public boolean synchronize = true;

    }

    public static class IndustrialCraft2 {

        @Config.Comment("Fixed an issue where some item repair recipes would duplication items.")
        @Config.LangKey("stellar_core.config.bugfixes.industrialCraft2.gradualRecipeFixes")
        @Config.Name("GradualRecipeFixes")
        public boolean gradualRecipe = true;

        @Config.Comment("Fixed an issue where the orientation determined by Ejector / Pulling Upgrade was the opposite of what it actually was.")
        @Config.LangKey("stellar_core.config.bugfixes.industrialCraft2.stackUtilInvFacingFixes")
        @Config.Name("StackUtilInvFacingFixes")
        public boolean stackUtilInvFacingFixes = true;

    }

    public static class InGameInfoXML {

        @Config.Comment("Fix the issue where a paragraph would pop up to report an error in a server environment, that's all.")
        @Config.LangKey("stellar_core.config.bugfixes.inGameInfoXML.playerHandlerFixes")
        @Config.Name("PlayerHandlerFixes")
        public boolean playerHandler = true;

    }

    public static class ImmersiveEngineering {

        @Config.Comment("Fixes an issue that would cause items to duplicate in certain special cases, although they were a bit tricky to reproduce.")
        @Config.LangKey("stellar_core.config.bugfixes.immersiveEngineering.multiblockStructureContainerFixes")
        @Config.Name("MultiblockStructureContainerFixes")
        public boolean blockIEMultiblock = true;

        @Config.Comment("Fixes an issue that would cause fluids to duplicate in some special cases, although they were a bit tricky to reproduce.")
        @Config.LangKey("stellar_core.config.bugfixes.immersiveEngineering.jerryCanFixes")
        @Config.Name("JerryCanFixes")
        public boolean fixJerryCanRecipe = true;

        @Config.Comment("Makes Excavator not drop twice drops when digging blocks (possible side effect).")
        @Config.LangKey("stellar_core.config.bugfixes.immersiveEngineering.tileEntityExcavatorDigBlockFixes")
        @Config.Name("TileEntityExcavatorDigBlockFixes")
        public boolean tileEntityExcavator = true;

        @Config.Comment({
                "Fixes an issue that caused ArcFurnace's item bar items to stack",
                "more than their items themselves under certain special circumstances, helping to fix item duplication."
        })
        @Config.LangKey("stellar_core.config.bugfixes.immersiveEngineering.tileEntityArcFurnaceInventoryFixes")
        @Config.Name("TileEntityArcFurnaceInventoryFixes")
        public boolean tileEntityArcFurnace = true;

        @Config.Comment({
                "(Client Only) Clear the model cache when the player switches worlds to avoid memory leaks.",
                "Minor performance impact. Mainly a problem when installing with other mods."
        })
        @Config.LangKey("stellar_core.config.bugfixes.immersiveEngineering.autoCleanRenderCache")
        @Config.Name("AutoCleanRenderCache")
        public boolean renderCache = true;

        @Config.Comment({
                "Immediately exit the thread when `Immersive Engineering Contributors Thread` encounters an error while reading JSON,",
                "instead of always printing the error."
        })
        @Config.LangKey("stellar_core.config.bugfixes.immersiveEngineering.ieContributorsThreadExceptionFixes")
        @Config.Name("IEContributorsThreadExceptionFixes")
        public boolean contributorSpecialsDownloader = true;

    }

    public static class JourneyMap {

        @Config.Comment("(Client Only) Automatically clears the radar player cache when a player switches worlds to avoid memory leaks caused in the client.")
        @Config.LangKey("stellar_core.config.bugfixes.journeyMap.autoCleanPlayerRadar")
        @Config.Name("AutoCleanPlayerRadar")
        public boolean playerRadar = true;

    }

    public static class LibVulpes {

        @Config.Comment({
                "(Client Only) Automatically clean up InputSyncHandler's spaceDown data when the player switches worlds.",
                "Although libvulpes has already coded this judgment, there is still a small chance that it will trigger a memory leak."
        })
        @Config.LangKey("stellar_core.config.bugfixes.libVulpes.autoCleanInputSyncHandlerData")
        @Config.Name("AutoCleanInputSyncHandlerData")
        public boolean inputSyncHandler = true;

    }

    public static class Mekanism {

        @Config.Comment({
                "(Client Only) Automatically clean up old player data when the player switches worlds to address some memory leaks,",
                "and while Mekanism has written cleanup features, they will only clean up when returning to the main menu."
        })
        @Config.LangKey("stellar_core.config.bugfixes.mekanism.autoCleanPortableTeleports")
        @Config.Name("AutoCleanPortableTeleports")
        public boolean portableTeleports = true;

    }

    public static class ModularRouters {

        @Config.Comment("Prevent routers from recognizing fluid bucket containers to avoid unexpected fluid replication problems.")
        @Config.LangKey("stellar_core.config.bugfixes.modularRouters.bufferHandlerFluidHandlerFixes")
        @Config.Name("BufferHandlerFluidHandlerFixes")
        public boolean bufferHandler = true;

    }

    public static class MoreElectricTools {

        @Config.Comment("Items such as Electric First Aid Life Support do not continue to work if the player has died.")
        @Config.LangKey("stellar_core.config.bugfixes.moreElectricTools.lifeSupportsFixes")
        @Config.Name("LifeSupportsFixes")
        public boolean fixLifeSupports = true;

    }

    public static class MrCrayfishFurniture {

        @Config.Comment("Stops the game from freezing in certain special cases, mainly occurring on photo frames and other similar blocks.")
        @Config.RequiresMcRestart
        @Config.LangKey("stellar_core.config.bugfixes.mrCrayfishFurniture.imageCacheCrashFixes")
        @Config.Name("ImageCacheCrashFixes")
        public boolean imageCache = true;

        @Config.Comment("Make blocks be rotated without losing their internal attributes and items (possibly not all blocks).")
        @Config.LangKey("stellar_core.config.bugfixes.mrCrayfishFurniture.rotatableFurniture")
        @Config.Name("RotatableFurniture")
        public boolean rotatableFurniture = false;

        @Config.Comment("Problem preventing certain container items from duplicate.")
        @Config.LangKey("stellar_core.config.bugfixes.mrCrayfishFurniture.blockFurnitureTileFixes")
        @Config.Name("BlockFurnitureTileFixes")
        public boolean blockFurnitureTile = true;

        @Config.Comment("Stopping washing machines from repairing non-repairable items.")
        @Config.LangKey("stellar_core.config.bugfixes.mrCrayfishFurniture.washingMachineDamageFixes")
        @Config.Name("WashingMachineDamageFixes")
        public boolean washingMachine = true;

    }

    public static class RPSIdeas {

        @Config.Comment("(Client Only) Fix memory leaks caused by improper object management on the client side.")
        @Config.LangKey("stellar_core.config.bugfixes.rpsIdeas.itemBioticSensorMemoryLeakFixes")
        @Config.Name("ItemBioticSensorMemoryLeakFixes")
        public boolean itemBioticSensor = true;

    }

    public static class ScalingGuis {

        @Config.Comment("Fixes an issue that caused a crash when deleting invalid GUI configurations.")
        @Config.LangKey("stellar_core.config.bugfixes.scalingGuis.jsonHelperCrashFixes")
        @Config.Name("JsonHelperCrashFixes")
        public boolean fixJsonHelper = true;

    }

    public static class Sync {

        @Config.Comment("A special fix that keeps Sync from triggering some weird item duplication issues when installed with Techguns.")
        @Config.LangKey("stellar_core.config.bugfixes.sync.techgunsDuplicationFixes")
        @Config.Name("TechgunsDuplicationFixes")
        public boolean techgunsDuplicationFixes = true;

        @Config.Comment("Make players get off their mounts when they die.")
        @Config.LangKey("stellar_core.config.bugfixes.sync.ridingFixes")
        @Config.Name("RidingFixes")
        public boolean ridingFixes = true;

    }

    public static class TConEvo {

        @Config.Comment({
                "Fix a special crash issue that would cause special cases,",
                "reporting that they were attributed to xyz.phanta.tconevo.integration.avaritia.client.AvaritiaMaterialModel$BakedAvaritiaMaterialModel$ WithoutHalo handleCosmicLighting()."
        })
        @Config.LangKey("stellar_core.config.bugfixes.tConEvo.handleCosmicLightingNPEFixes")
        @Config.Name("HandleCosmicLightingNPEFixes")
        public boolean handleCosmicLightingNPEFixes = true;

    }

    public static class Techguns {

        @Config.Comment("Fixes an issue that would cause crashes in server environments.")
        @Config.LangKey("stellar_core.config.bugfixes.techguns.tgPermissionsCrashFixes")
        @Config.Name("TGPermissionsCrashFixes")
        public boolean tgPermissions = true;

        @Config.Comment("Fix for recipes not working properly for certain items (Techguns only).")
        @Config.LangKey("stellar_core.config.bugfixes.techguns.invalidRecipeFixes")
        @Config.Name("InvalidRecipeFixes")
        public boolean fixAmmoSumRecipeFactory = true;

        @Config.Comment("Fixes an issue that would cause crashes in server environments.")
        @Config.LangKey("stellar_core.config.bugfixes.techguns.serverSideEntityCrashFixes")
        @Config.Name("ServerSideEntityCrashFixes")
        public boolean serverSideEntityCrashFixes = true;

    }

    public static class TheOneProbe {

        @Config.Comment("Prevents TheOneProbe from rendering the entity/player in such a way that their head is locked to a fixed pitch.")
        @Config.LangKey("stellar_core.config.bugfixes.theOneProbe.playerEntityRenderFixes")
        @Config.Name("PlayerEntityRenderFixes")
        public boolean fixRenderHelper = true;

    }

    public static class ThermalDynamics {

        @Config.Comment("Fix a fluid duplication issue where they would only appear on Super-Laminar FluidDuct.")
        @Config.LangKey("stellar_core.config.bugfixes.thermalDynamics.fluidDuplicateFixes")
        @Config.Name("FluidDuplicateFixes")
        public boolean fixFluidDuplicate = true;

    }

    public static class ThermalExpansion {

        @Config.Comment("The problem with stopping a backpack from replicating is that this probably does the same thing as UniversalTweaks.")
        @Config.LangKey("stellar_core.config.bugfixes.thermalExpansion.containerSatchelFilterFixes")
        @Config.Name("ContainerSatchelFilterFixes")
        public boolean containerSatchelFilter = true;

    }

}
