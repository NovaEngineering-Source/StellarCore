package github.kasuminova.stellarcore.common.config.category;

import net.minecraftforge.common.config.Config;

public class Performance {

    @Config.Name("Vanilla")
    public final Vanilla vanilla = new Vanilla();

    @Config.Name("Forge")
    public final Forge forge = new Forge();

    @Config.Name("AstralSorcery")
    public final AstralSorcery astralSorcery = new AstralSorcery();

    @Config.Name("Avaritia")
    public final Avaritia avaritia = new Avaritia();

    @Config.Name("BiomesOPlenty")
    public final BiomesOPlenty biomesOPlenty = new BiomesOPlenty();

    @Config.Name("EnderUtilities")
    public final EnderUtilities enderUtilities = new EnderUtilities();

    @Config.Name("ExtraBotany")
    public final ExtraBotany extraBotany = new ExtraBotany();

    @Config.Name("BloodMagic")
    public final BloodMagic bloodMagic = new BloodMagic();

    @Config.Name("Botania")
    public final Botania botania = new Botania();

    @Config.Name("Chisel")
    public final Chisel chisel = new Chisel();

    @Config.Name("CTM")
    public final CTM ctm = new CTM();

    @Config.Name("Cucumber")
    public final Cucumber cucumber = new Cucumber();

    @Config.Name("CustomLoadingScreen")
    public final CustomLoadingScreen customLoadingScreen = new CustomLoadingScreen();

    @Config.Name("EBWizardry")
    public final EBWizardry ebWizardry = new EBWizardry();

    @Config.Name("EnderCore")
    public final EnderCore enderCore = new EnderCore();

    @Config.Name("EnderIO")
    public final EnderIO enderIO = new EnderIO();

    @Config.Name("EnderIOConduits")
    public final EnderIOConduits enderIOConduits = new EnderIOConduits();

    @Config.Name("FluxNetworks")
    public final FluxNetworks fluxNetworks = new FluxNetworks();

    @Config.Name("FTBLib")
    public final FTBLib ftbLib = new FTBLib();

    @Config.Name("FTBQuests")
    public final FTBQuests ftbQuests = new FTBQuests();

    @Config.Name("IndustrialCraft2")
    public final IndustrialCraft2 industrialCraft2 = new IndustrialCraft2();

    @Config.Name("InGameInfoXML")
    public final InGameInfoXML inGameInfoXML = new InGameInfoXML();

    @Config.Name("ImmersiveEngineering")
    public final ImmersiveEngineering immersiveEngineering = new ImmersiveEngineering();

    @Config.Name("LibNine")
    public final LibNine libNine = new LibNine();

    @Config.Name("Mekanism")
    public final Mekanism mekanism = new Mekanism();

    @Config.Name("NuclearCraftOverhauled")
    public final NuclearCraftOverhauled nuclearCraftOverhauled = new NuclearCraftOverhauled();

    @Config.Name("TConstruct")
    public final TConstruct tConstruct = new TConstruct();

    @Config.Name("TouhouLittleMaid")
    public final TouhouLittleMaid tlm = new TouhouLittleMaid();

    public static class Vanilla {

        @Config.Comment({
                "(Client Performance | Experimental) A feature from Patcher mod, using protocol CC-BY-NC-SA 4.0, if there are any copyright issues, please contact me to remove it.",
                "Dramatically improves performance by limiting the HUD to a specified FPS, may not be compatible with older devices.",
                "May perform strangely with some HUD Mods."
        })
        @Config.RequiresMcRestart
        @Config.Name("HudCaching")
        public boolean hudCaching = false;

        @Config.Comment("Select a restricted HUD FPS that is only valid when HudCaching is enabled.")
        @Config.RangeInt(min = 5, max = 240)
        @Config.Name("HudCachingFPSLimit")
        public int hudCachingFPSLimit = 20;

        @Config.Comment({
                "(Client Performance | Experimental) A feature that helps speed up game loading by modifying the model loader's code to enable parallel loading capabilities (5s ~ 40s faster).",
                "Incompatible with some mod's models because they use their own model loader, if you encounter a missing model, please report it to the StellarCore author for manual compatibility.",
                "Compatible model loader: CTM，LibNine，TConstruct",
                "Contrary to VintageFix's DynamicResource functionality and therefore incompatible, you can only choose one."
        })
        @Config.RequiresMcRestart
        @Config.Name("ParallelModelLoader")
        public boolean parallelModelLoader = true;

        @Config.Comment({
                "(Client Performance) Clearing the cache after loading a model, significantly reduce memory usage.",
                "But it may cause some mod's models to be messed up after reloading ResourcePacks,",
                "Turning this option off will use more memory.",
                "If you installed FoamFix, FoamFix does the same thing but StellarCore is faster, you may need to turn off the `wipeModelCache` option in foamfix.cfg."
        })
        @Config.RequiresMcRestart
        @Config.Name("WipeModelCache")
        public boolean wipeModelCache = true;

        @Config.Comment({
                "Defining which ModelLoader cannot be safely asynchronized to allow StellarCore to load models",
                "using a synchronous approach, usually requires no modification to it."
        })
        @Config.RequiresMcRestart
        @Config.Name("ParallelModelLoaderBlackList")
        public String[] parallelModelLoaderBlackList = {"slimeknights.tconstruct.library.client.model.ModifierModelLoader"};

        @Config.Comment({
                "(Client Performance | Experimental) An feature that uses parallel loading of texture files, improved game loading speed.",
                "If you get a crash when installing with VintageFix, turn this feature off, or turn off the mixins.texturemap option for VintageFix."
        })
        @Config.RequiresMcRestart
        @Config.Name("ParallelTextureLoad")
        public boolean parallelTextureLoad = false;

        @Config.Comment("(Client Performance) Improve the Map data structures of StateMapperBase to make them faster (~30%).")
        @Config.RequiresMcRestart
        @Config.Name("StateMapperBaseImprovements")
        public boolean stateMapperBase = true;

        @Config.Comment("(Server Performance) Modified the data structure of capturedBlockSnapshots to a LinkedList to help improve insertion and deletion performance.")
        @Config.RequiresMcRestart
        @Config.Name("CapturedBlockSnapshotsImprovements")
        public boolean capturedBlockSnapshots = false;

        @Config.Comment({
                "(Client/Server Performance) Use long instead of BlockPos to store TileEntities, optimising memory usage and potentially improving performance.",
                "Conflicts with UniversalTweaks - 'Tile Entity Map' options and StellarCore maybe overrides them."
        })
        @Config.RequiresMcRestart
        @Config.Name("ChunkTileEntityMapImprovements")
        public boolean chunkTEMap = false;

        @Config.Comment("(Client/Server Performance | Experimental) Cache the TileEntity state of the IBlockState in a chunk to improve performance.")
        @Config.RequiresMcRestart
        @Config.Name("ChunkTileEntityCache")
        public boolean chunkTECache = false;

        @Config.Comment("(Client/Server Performance) Improving Chunk Performance with Improved Data Structures.")
        @Config.RequiresMcRestart
        @Config.Name("ChunkTileEntityQueueImprovements")
        public boolean chunkTEQueue = true;

        @Config.Comment("(Server Performance) Improving the performance of ClassInheritanceMultiMap (up to ~40%).")
        @Config.RequiresMcRestart
        @Config.Name("ClassInheritanceMultiMapImprovements")
        public boolean classMultiMap = true;

        @Config.Comment("(Server Performance) Improving EntityTracker Performance with Improved Data Structures.")
        @Config.RequiresMcRestart
        @Config.Name("EntityTrackerImprovements")
        public boolean entitytracker = true;

        @Config.Comment("(Server Performance) Improving WorldServer#getPendingBlockUpdates Performance with Improved Data Structures.")
        @Config.RequiresMcRestart
        @Config.Name("WorldServerGetPendingBlockUpdatesImprovements")
        public boolean worldServerGetPendingBlockUpdates = true;

        @Config.Comment("(Client Performance) Improving PropertyEnum#hashCode Performance with hashCode cache.")
        @Config.RequiresMcRestart
        @Config.Name("PropertyEnumHashCodeCache")
        public boolean propertyEnumHashCodeCache = true;

        @Config.Comment("(Server Performance) Improving BlockStateContainer$BlockStateImplementation#hashCode Performance with hashCode cache.")
        @Config.RequiresMcRestart
        @Config.Name("BlockStateImplementationHashCodeCache")
        public boolean blockStateImplementationHashCodeCache = true;

        @Config.Comment({
                "(Client/Server Performance) Improve the data structure of NBTTagCompound and NBTTagList and optimise the performance of matching, fetching and copying.",
                "May conflict with other mods optimised for NBT.",
                "Known to conflict with CensoredASM's `optimizeNBTTagCompoundBackingMap` and `nbtBackingMapStringCanonicalization` option.",
        })
        @Config.RequiresMcRestart
        @Config.Name("NBTTagImprovements")
        public boolean nbtTag = true;

        @Config.Comment({
                "(Client/Server Performance) Cache constants -32768 - 32767 of NBTTagByte, NBTTagInt, NBTTagLong, NBTTagFloat, NBTTagDouble using constant pool.",
                "Like IntegerCache in the JVM, improves memory usage and reduces object creation overhead.",
                "Incompatible with old version of Quark (< r1.6-189), which modifies the bytecode of the NBTTag class too early.",
        })
        @Config.RequiresMcRestart
        @Config.Name("NBTPrimitiveConstantsPool")
        public boolean nbtPrimitiveConstantsPool = true;

        @Config.Comment({
                "(Client/Server Performance | Experimental) Asynchronous loading of ItemStack's Capability to improve performance.",
                "Conflict with CensoredASM's `delayItemStackCapabilityInit` option."
        })
        @Config.RequiresMcRestart
        @Config.Name("AsyncItemStackCapabilityInit")
        public boolean asyncItemStackCapabilityInit = false;

        @Config.Comment("(Client/Server Performance | Experimental) Replaces the internal default ArrayList of NonNullList with an ObjectArrayList (may not work).")
        @Config.RequiresMcRestart
        @Config.Name("NonNullListImprovements")
        public boolean nonNullList = true;

        @Config.Comment("(Client Performance) As the configuration name says, use at your own risk.")
        @Config.RequiresMcRestart
        @Config.Name("NoGLError")
        public boolean noGlError = false;

        @Config.Comment({
                "(Client Performance | Sodium Feature) Making all immediate chunk updates always deferred helps improve intermittent ",
                "low FPS conditions, but potentially leads to rendering delays."
        })
        @Config.RequiresMcRestart
        @Config.Name("AlwaysDeferChunkUpdates")
        public boolean alwaysDeferChunkUpdates = false;

        @Config.Comment({
                "(Client Performance) Enabling Stitcher caching improves the game loading speed.",
                "The main principle is to cache the Stitcher's splicing results and save them to the hard drive for next time reading, so",
                "you need to pre-launch the game once before you can see the effect.",
                "Not compatible with VintageFix's DynamicResource, but should work well with VintageFix's TurboStitcher."
        })
        @Config.RequiresMcRestart
        @Config.Name("StitcherCache")
        public boolean stitcherCache = false;

        @Config.Comment({
                "(Client Performance) Caches the state of existence of each resource file in the ResourcePack,",
                "improve the speed of model loading, if you encounter the game can not be loaded or display anomaly, turn off this option."
        })
        @Config.RequiresMcRestart
        @Config.Name("ResourceExistStateCache")
        public boolean resourceExistStateCache = true;

        @Config.Comment({
                "(Client/Server Performance) Use parallelStream to handle randomTick operations on world blocks to improve performance in more player environments.",
                "Note: Possibly affecting the random logic of the original game."
        })
        @Config.RequiresMcRestart
        @Config.Name("ParallelRandomBlockTicker")
        public boolean parallelRandomBlockTicker = false;

        @Config.Comment("(Client/Server Performance) Improved `World#isValid` / `World#isOutsideBuildHeight` judgement performance, minor performance improvements.")
        @Config.RequiresMcRestart
        @Config.Name("WorldBlockPosJudgement")
        public boolean worldBlockPosJudgement = true;

        @Config.RequiresMcRestart
        @Config.Comment("(Client Performance) Improved BlockPart data structure, improve memory usage with a more efficient map.")
        @Config.Name("BlockPartDataStructureImprovements")
        public boolean blockPartDataStructure = true;

        @Config.RequiresMcRestart
        @Config.Comment({
                "(Client Performance) Modify the data structure of ModelBlock's textures map to improve performance and reduce memory usage.",
                "This feature requires CensoredASM mod.",
                "Known to be incompatible with DynamicTrees."
        })
        @Config.Name("ModelBlockStringCanonicalization")
        public boolean modelBlockStringCanonicalization = false;

        @Config.Comment({
                "(Client Performance | Experimental) Deduplicate vertexData array to optimise memory usage.",
                "Works in most cases, but may cause rendering issues with models in some mods."
        })
        @Config.RequiresMcRestart
        @Config.Name("BakedQuadVertexDataCanonicalization")
        public boolean bakedQuadVertexDataCanonicalization = false;

        @Config.Comment({
                "(Client Performance | Experimental) BakedQuad deduplication of SimpleBakedModel to optimise memory usage.",
                "Works in most cases, but may cause rendering issues with models in some mods."
        })
        @Config.RequiresMcRestart
        @Config.Name("SimpleBakedModelCanonicalization")
        public boolean simpleBakedModelCanonicalization = false;

        @Config.Comment("(Client Performance) Deduplicate BlockFaceUV `uvs` array to optimise memory usage.")
        @Config.RequiresMcRestart
        @Config.Name("BlockFaceUVsCanonicalization")
        public boolean blockFaceUVsCanonicalization = true;

        @Config.Comment({
                "(Client/Server Performance) Deduplicate internal strings of ResourceLocation to reduce memory usage.",
                "When installed with CensoredASM, turn off the `resourceLocationCanonicalization` feature of CensoredASM.",
                "StellarCore already has backend integration for it.",
                "Note: This feature may have a large impact on load times."
        })
        @Config.RequiresMcRestart
        @Config.Name("ResourceLocationCanonicalization")
        public boolean resourceLocationCanonicalization = true;

        @Config.Comment({
                "(Client/Server Performance) ResourceLocationCanonicalization Available when enabled, makes the operation process asynchronous,",
                "dramatically reduces the impact on startup time, but uses more memory (mainly in client model loading, very much more memory) during loading,",
                "and the memory returns to normal after loading is complete."
        })
        @Config.RequiresMcRestart
        @Config.Name("ResourceLocationCanonicalizationAsync")
        public boolean resourceLocationCanonicalizationAsync = false;

    }

    public static class Forge {

        @Config.Comment({
                "(Client/Server Performance) ASMDataTable Annotation Map builds use half of the CPU instead of all of it,",
                "helping to improve the computer freezing problem at game startup, but potentially causing the game to take longer to load."
        })
        @Config.RequiresMcRestart
        @Config.Name("ASMDataTableCPUUsageImprovements")
        public boolean asmDataTable = false;

        @Config.Comment("(Client/Server Performance) ChunkManager optimisation, improves performance in more player environments.")
        @Config.RequiresMcRestart
        @Config.Name("ChunkManager")
        public boolean chunkManager = true;

        @Config.Comment({
                "(Client Performance | Experimental) Deduplicate unpackedData array to optimise memory usage, with significant optimisation for some mods.",
                "Works in most cases, but may cause rendering issues with models in some mods."
        })
        @Config.RequiresMcRestart
        @Config.Name("UnpackedBakedQuadDataCanonicalization")
        public boolean unpackedBakedQuadDataCanonicalization = false;

        @Config.Comment({
                "Adjust the optimisation level of the `UnpackedBakedQuadDataCanonicalization` option, the higher the level",
                "the better the results but the higher the probability of encountering problems, normally a setting of 2 is sufficient...",
                "Higher levels consume more CPU performance.",
                "This option can be adjusted while the game is running, but restarting the game is highly recommended."
        })
        @Config.SlidingOption
        @Config.RequiresMcRestart
        @Config.RangeInt(min = 1, max = 3)
        @Config.Name("UnpackedBakedQuadDataCanonicalizationLevel")
        public int unpackedBakedQuadDataCanonicalizationLevel = 1;

        @Config.Comment({
                "(Client Performance | Experimental) Deduplicate vertexData array to optimise memory usage.",
                "Works in most cases, but may cause rendering issues with models in some mods."
        })
        @Config.RequiresMcRestart
        @Config.Name("UnpackedBakedQuadVertexDataCanonicalization")
        public boolean unpackedBakedQuadVertexDataCanonicalization = false;

        @Config.Comment("When writing to Capability's NBT, if the returned NBT is empty, no content is written, which may help improve performance.")
        @Config.RequiresMcRestart
        @Config.Name("DeallocateEmptyCapabilityNBT")
        public boolean deallocateEmptyCapabilityNBT = true;

    }

    public static class AstralSorcery {

        @Config.Comment("(Server Performance) Add optional updates to the block to improve network bandwidth usage.")
        @Config.Name("TileNetworkSkyboundImprovements")
        public boolean tileNetworkSkybound = true;

    }

    public static class Avaritia {

        @Config.Comment("(Server Performance) Removing some unnecessary Server to Client synchronization helps ease network bandwidth usage.")
        @Config.Name("TileBaseImprovements")
        public boolean tileBase = true;

        @Config.Comment("(Client / Server Performance) Speed up recipe loading with parallel loading.")
        @Config.Name("AvaritiaRecipeManagerImprovements")
        public boolean avaritiaRecipeManager = true;

    }

    public static class BiomesOPlenty {

        @Config.Comment("(Client/Server Performance) Block them from doing network operations in the main thread.")
        @Config.Name("TrailManagerAsync")
        public boolean trailManager = true;

    }

    public static class Cucumber {

        @Config.Comment("(Client/Server Performance) Block them from doing network operations in the main thread.")
        @Config.Name("VanillaPacketDispatcherImprovements")
        public boolean vanillaPacketDispatcher = false;

        @Config.Comment({
                "When a block is updated, how many players within range can receive its update?",
                "Only works if VanillaPacketDispatcherImprovements is enabled, and only works on mods that use the Cucumber lib."
        })
        @Config.Name("TileEntityUpdateRange")
        public float tileEntityUpdateRange = 16F;

    }

    public static class EnderUtilities {

        @Config.Comment("(Server Performance) Improvements to the way UtilItemModular loads items to slightly improve performance.")
        @Config.Name("UtilItemModularImprovements")
        public boolean utilItemModular = true;

    }

    public static class ExtraBotany {

        @Config.Comment("(Client/Server Performance) Block them from doing network operations in the main thread.")
        @Config.Name("PersistentVariableHandlerAsync")
        public boolean persistentVariableHandler = true;

    }

    public static class BloodMagic {

        @Config.Comment("(Server Performance) Removing some unnecessary Server to Client synchronization helps ease network bandwidth usage.")
        @Config.Name("BloodAltarImprovements")
        public boolean bloodAltar = true;

    }

    public static class Botania {

        @Config.Comment("(Server Performance) A feature with some side effects to make sparks use less performance through dynamic Tick acceleration.")
        @Config.Name("SparkEntityImprovements")
        public boolean sparkImprovements = false;

        @Config.Comment("(Server Performance) Improvements to the way Alf Portals work to slightly improve performance.")
        public boolean alfPortalImprovements = true;

        @Config.Comment("(Server Performance) Improvements to the way Pylons work to slightly improve performance.")
        public boolean pylonImprovements = true;

        @Config.Comment("(Server Performance) Improvements to the way Rune Altars work to slightly improve performance.")
        public boolean runeAltarImprovements = true;

        @Config.Comment({
                "What is the maximum working interval of the sparks? They will eventually be accelerated to 1 tick.",
                "Only works if SparkEntityImprovements is enabled."
        })
        @Config.RangeInt(min = 2, max = 60)
        @Config.Name("SparkMaxWorkDelay")
        public int sparkMaxWorkDelay = 20;

    }

    public static class Chisel {

        @Config.Comment({
                "(Server Performance) A feature with some side effects that improves the performance of Auto Chisel's recipe search",
                "and makes the interval between searches for recipes increase."
        })
        @Config.Name("AutoChiselImprovements")
        public boolean autoChiselImprovements = true;

        @Config.Comment({
                "What is the maximum recipe search interval of the Auto Chisels? They will eventually be accelerated to 20 tick.",
                "Only works if AutoChiselImprovements is enabled."
        })
        @Config.RangeInt(min = 20, max = 100)
        @Config.Name("AutoChiselMaxWorkDelay")
        public int autoChiselMaxWorkDelay = 100;

    }

    public static class CTM {

        @Config.Comment({
                "(Client Performance | Experimental) A feature that loads CTM's Metadata data faster (~60%) using parallelStream,",
                "usually with few conflict issues. If enabling this feature causes a problem, please report it immediately."
        })
        @Config.RequiresMcRestart
        @Config.Name("TextureMetadataHandlerImprovements")
        public boolean textureMetadataHandler = false;

    }

    public static class CustomLoadingScreen {

        @Config.Comment("(Client Performance) Clean up their mapping after the game has finished loading to improve memory usage.")
        @Config.RequiresMcRestart
        @Config.Name("TextureCleanup")
        public boolean splashProgress = true;

        @Config.Comment("(Recommend) (Client Performance) We'll never know why we have to wait an extra (20*5)ms for each module loaded.")
        @Config.RequiresMcRestart
        @Config.Name("ModLoadingListenerImprovements")
        public boolean modLoadingListener = true;

    }

    public static class EBWizardry {

        @Config.Comment({
                "(Server Performance) Improved event listening performance for DispenserCastingData, required mc restart.",
                "Incompatible with TickCentral mod, alternative optimisations are used when installing with this mod.",
        })
        @Config.RequiresMcRestart
        @Config.Name("DispenserCastingDataImprovements")
        public boolean dispenserCastingData = false;

    }

    public static class EnderCore {

        @Config.Comment({
                "(Server Performance) Improve the speed of matching materials such as items using caching and special data structures",
                "to improve the performance of EnderIO Machines overall, with a slight increase in memory usage."
        })
        @Config.Name("ThingsImprovements")
        public boolean things = true;

        @Config.Comment({
                "(Server Performance) Improve the speed of matching materials such as items using caching and special data structures",
                "to improve the performance of EnderIO Machines overall, with a slight increase in memory usage."
        })
        @Config.Name("OreThingImprovements")
        public boolean oreThing = true;

    }

    public static class EnderIO {

        @Config.Comment("(Server Performance) Removing some unnecessary parts to improve performance, may affect the use of the Profiler.")
        @Config.Name("ItemToolsImprovements")
        public boolean itemTools = true;

        @Config.Comment("(Server Performance) Remove some unnecessary judgments to improve performance (may have side effects).")
        @Config.Name("TileEntityBaseImprovements")
        public boolean tileEntityBase = true;

        @Config.Comment("(Server Performance) Improve recipe search speed with caching.")
        @Config.Name("RecipeImprovements")
        public boolean recipe = true;

        @Config.Comment("(Server Performance) Improve the performance of item determination in FarmerStation using caching (mainly related to the canPlant() method).")
        @Config.Name("FarmerImprovements")
        public boolean commune = true;

    }

    public static class EnderIOConduits {

        @Config.Comment("(Server Performance) Removing some unnecessary parts to improve performance, may affect the use of the Profiler.")
        @Config.Name("AbstractConduitImprovements")
        public boolean abstractConduit = true;

        @Config.Comment("(Server Performance) Removing some unnecessary parts to improve performance, may affect the use of the Profiler.")
        @Config.Name("TileConduitBundleImprovements")
        public boolean tileConduitBundle = true;

        @Config.Comment("(Server Performance) Improved the hashCode() method of NetworkTankKey, which can improve the performance of the EnderIO Conduit Network.")
        @Config.Name("NetworkTankKeyHashCodeCache")
        public boolean networkTankKeyHashCodeCache = true;

        @Config.Comment("(Server Performance) Improved some data structures, slight performance improvements.")
        @Config.Name("EnderLiquidConduitNetworkTankMap")
        public boolean enderLiquidConduitNetworkTankMap = true;

        @Config.Comment("(Server Performance | Experimental) Rewriting the eio conduit energy network computation logic to improve performance using multithreading.")
        @Config.Name("NetworkPowerManagerImprovements")
        public boolean networkPowerManager = true;

    }

    public static class FluxNetworks {

        @Config.Comment("(Server Performance | Experimental) Rewriting the flux network calculation logic to improve performance using multithreading.")
        @Config.Name("ParallelNetworkCalculation")
        public boolean parallelNetworkCalculation = false;

        @Config.Comment("(Server Performance) Removing the secondary judgement of energy transfer may help improve performance.")
        @Config.Name("ConnectionTransferImprovements")
        public boolean connectionTransfer = true;

    }

    public static class FTBLib {

        @Config.Comment("(Server Performance) Improved some of the judgments so that it doesn't consume a lot of time sending network packets.")
        @Config.Name("InvUtilsForceUpdateImprovements")
        public boolean invUtilForceUpdate = true;

    }

    public static class FTBQuests {

        @Config.Comment("(Server Performance) Improved performance of item quest checking (but may result in longer intervals between quest checks).")
        @Config.Name("QuestInventoryListenerImprovements")
        public boolean questInventoryListener = false;

    }

    public static class IndustrialCraft2 {

        @Config.RequiresMcRestart
        @Config.Comment("(Server Performance | Experimental) Rewriting the ic2 energy network computation logic to improve performance using multithreading.")
        @Config.Name("EnergyCalculatorLegImprovements")
        public boolean energyCalculatorLeg = true;

        @Config.Comment("(Server Performance) Improved some data structures, slight performance improvements.")
        @Config.Name("GridDataImprovements")
        public boolean energyCalculatorLegGridData = true;

        @Config.Comment("(Server Performance) Improved some data structures, slight performance improvements.")
        @Config.Name("EnergyNetLocalImprovements")
        public boolean energyNetLocal = true;

//        @Config.Comment("(Server Performance) Improve EnergyNetLocal#getIoTile and EnergyNetLocal#getSubTile fetching speed to optimise performance to some extent.")
//        @Config.Name("GetIoAndSubTileEnergyNetLocalImprovements")
//        public boolean getIoAndSubTile = true;

        @Config.Comment("(Server Performance) Improved some data structures, slight performance improvements.")
        @Config.Name("GridImprovements")
        public boolean grid = true;

        @Config.Comment("(Server Performance) Allows you to adjust the working speed of the Ejector / Pulling Module.")
        @Config.Name("ItemUpgradeModuleImprovements")
        public boolean itemUpgradeModule = false;

        @Config.Comment({
                "Work speed of Ejector / Pulling Module.",
                "Only works if ItemUpgradeModuleImprovements is enabled."
        })
        @Config.Name("ItemUpgradeModuleWorkDelay")
        public int itemUpgradeModuleWorkDelay = 5;

    }

    public static class InGameInfoXML {

        @Config.Comment({
                "(Client Performance) Limit the rendering FPS of InGameInfoXML to significantly improve performance (similar to HUDCaching),",
                "may not be compatible with older devices."
        })
        @Config.Name("HUDFramebuffer")
        public boolean hudFrameBuffer = false;

        @Config.Name("HUDFPS")
        @Config.Comment("Select a restricted HUD FPS that is only valid when HUDFramebuffer is enabled.")
        @Config.RangeInt(min = 5, max = 60)
        public int hudFrameRate = 10;

    }

    public static class ImmersiveEngineering {

        @Config.Comment({
                "(Server Performance) Blocking the IE Mechanical Block from triggering a full block update when transferring energy may improve performance.",
                "But if strange block states appear try turning off this option."
        })
        @Config.Name("EnergyTransferNoUpdate")
        public boolean energyTransferNoUpdate = true;

    }

    public static class LibNine {

        @Config.Comment({
                "(Client Performance) Cache the result of L9Models#isOfType to improve game loading speed.",
                "This feature requires Vanilla#ResourceExistStateCache option."
        })
        @Config.Name("L9ModelsIsOfTypeCache")
        public boolean l9ModelsIsOfTypeCache = true;

    }

    public static class Mekanism {

        @Config.Comment({
                "(Server Performance) Performance improvements on data structures.",
                "MEKCEu already includes this feature, so installing MEKCEu will automatically disable it."
        })
        @Config.Name("PipeUtilsImprovements")
        public boolean pipeUtils = true;

        @Config.Comment({
                "(Server Performance) Performance improvements on data structures.",
                "MEKCEu already includes this feature, so installing MEKCEu will automatically disable it."
        })
        @Config.Name("EnergyNetworkImprovements")
        public boolean energyNetwork = true;

        @Config.Comment({
                "(Server Performance) Performance improvements on data structures.",
                "MEKCEu already includes this feature, so installing MEKCEu will automatically disable it."
        })
        @Config.Name("FrequencyImprovements")
        public boolean frequency = true;

    }

    public static class NuclearCraftOverhauled {

        @Config.Comment({
                "(Server Performance) Improvements search performance of basic recipes.",
                "Requires disable processor.smart_processor_input option at nuclearcraft.cfg."
        })
        @Config.Name("BasicRecipeSearchImprovements")
        public boolean basicRecipeImprovements = true;

    }

    public static class TConstruct {

        @Config.Comment("(Server Performance) Improvements in the search performance of Melting recipes.")
        @Config.Name("MeltingRecipeSearchImprovements")
        public boolean meltingRecipeSearch = true;

        @Config.Comment("(Server Performance) Improvements in the search performance of Table Casing recipes.")
        @Config.Name("TableCastingRecipeSearchImprovements")
        public boolean tableCastingSearch = true;

        @Config.Comment("(Server Performance) Improvements in the search performance of Basin Casing recipes.")
        @Config.Name("BasinCastingRecipeSearchImprovements")
        public boolean basinCastingSearch = true;

        @Config.Comment("(Server Performance) Improvements in the search performance of Smeltery Alloy Casing recipes.")
        @Config.Name("TileSmelteryAlloyRecipeSearchImprovements")
        public boolean tileSmelteryAlloyRecipeSearch = true;

        @Config.Comment("(Server Performance) Smeltery What is the maximum number of recipes that can be completed per tick?")
        @Config.RangeInt(min = 1, max = 100)
        @Config.Name("TileSmelteryMaxAlloyRecipePerTick")
        public int tileSmelteryMaxAlloyRecipePerTick = 5;

    }

    public static class TouhouLittleMaid {

        @Config.Comment("(Client Performance) Enable model data Canonicalization to improve TLM model memory usage.")
        @Config.RequiresMcRestart
        public boolean modelDataCanonicalization = true;

        @Config.Comment("(Client Performance) Enable TexturedQuadFloat data Canonicalization to improve TLM model memory usage.")
        @Config.RequiresMcRestart
        public boolean texturedQuadFloatCanonicalization = true;

    }

}
