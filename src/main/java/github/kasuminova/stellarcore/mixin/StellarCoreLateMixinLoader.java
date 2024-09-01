package github.kasuminova.stellarcore.mixin;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.mod.Mods;
import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.*;
import java.util.function.BooleanSupplier;

@SuppressWarnings({"unused", "SameParameterValue"})
public class StellarCoreLateMixinLoader implements ILateMixinLoader {

    private static final Map<String, BooleanSupplier> MIXIN_CONFIGS = new LinkedHashMap<>();

    static {
        addModdedMixinCFG("mixins.stellar_core_advancedrocketry.json",     "advancedrocketry");
        addModdedMixinCFG("mixins.stellar_core_ae.json",                   "appliedenergistics2");
        addModdedMixinCFG("mixins.stellar_core_ancientspellcraft.json",    "ancientspellcraft");
        addModdedMixinCFG("mixins.stellar_core_armourers_workshop.json",   "armourers_workshop");
        addModdedMixinCFG("mixins.stellar_core_astralsorcery.json",        "astralsorcery");
        addModdedMixinCFG("mixins.stellar_core_avaritia.json",             "avaritia");
        addModdedMixinCFG("mixins.stellar_core_avaritiaddons.json",        "avaritiaddons");
        addModdedMixinCFG("mixins.stellar_core_betterchat.json",           "betterchat");
        addModdedMixinCFG("mixins.stellar_core_biomesoplenty.json",        "biomesoplenty");
        addModdedMixinCFG("mixins.stellar_core_bloodmagic.json",           "bloodmagic");
        addModdedMixinCFG("mixins.stellar_core_botania.json",              "botania");
        addModdedMixinCFG("mixins.stellar_core_cfm.json",                  "cfm", () -> StellarCoreConfig.BUG_FIXES.mrCrayfishFurniture.imageCache);
        addModdedMixinCFG("mixins.stellar_core_chisel.json",               "chisel");
        addModdedMixinCFG("mixins.stellar_core_cofhcore.json",             "cofhcore");
        addModdedMixinCFG("mixins.stellar_core_ctm.json",                  "ctm", () -> StellarCoreConfig.PERFORMANCE.ctm.textureMetadataHandler);
        addModdedMixinCFG("mixins.stellar_core_cucumber.json",             "cucumber");
        addModdedMixinCFG("mixins.stellar_core_customloadingscreen.json",  "customloadingscreen");
        addModdedMixinCFG("mixins.stellar_core_customstartinggear.json",   "customstartinggear");
        addModdedMixinCFG("mixins.stellar_core_draconicevolution.json",    "draconicevolution");
        addModdedMixinCFG("mixins.stellar_core_ebwizardry.json",           "ebwizardry");
        addModdedMixinCFG("mixins.stellar_core_endercore.json",            "endercore");
        addModdedMixinCFG("mixins.stellar_core_enderio.json",              "enderio");
        addModdedMixinCFG("mixins.stellar_core_enderioconduits.json",      "enderio", "enderioconduits");
        addModdedMixinCFG("mixins.stellar_core_enderutilities.json",       "enderutilities");
        addModdedMixinCFG("mixins.stellar_core_extrabotany.json",          "extrabotany");
        addModdedMixinCFG("mixins.stellar_core_fluxnetworks.json",         "fluxnetworks");
        addModdedMixinCFG("mixins.stellar_core_ftblib.json",               "ftblib");
        addModdedMixinCFG("mixins.stellar_core_ftbquests.json",            "ftbquests");
        addModdedMixinCFG("mixins.stellar_core_guguutils.json",            "gugu-utils");
        addModdedMixinCFG("mixins.stellar_core_ic2.json",                  "ic2");
        addModdedMixinCFG("mixins.stellar_core_igi.json",                  "ingameinfoxml");
        addModdedMixinCFG("mixins.stellar_core_immersiveengineering.json", "immersiveengineering");
        addModdedMixinCFG("mixins.stellar_core_jei.json",                  "jei", () -> StellarCoreConfig.PERFORMANCE.vanilla.stitcherCache);
        addModdedMixinCFG("mixins.stellar_core_lazyae2.json",              "lazyae2");
        addModdedMixinCFG("mixins.stellar_core_legendarytooltips.json",    "legendarytooltips");
        addModdedMixinCFG("mixins.stellar_core_libnine.json",              "libnine", () -> StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader);
        addModdedMixinCFG("mixins.stellar_core_mek_top.json",        new String[]{"mekanism", "theoneprobe"}, () -> StellarCoreConfig.FEATURES.mekanism.topSupport);
        addMixinCFG(      "mixins.stellar_core_mekanism.json",                    () -> Mods.MEK.loaded() && !Mods.MEKCEU.loaded());
        addModdedMixinCFG("mixins.stellar_core_mets.json",                 "mets");
        addModdedMixinCFG("mixins.stellar_core_modularrouters.json",       "modularrouters");
        addModdedMixinCFG("mixins.stellar_core_nco.json",                  "nuclearcraft");
        addModdedMixinCFG("mixins.stellar_core_rgb_chat.json",             "jianghun");
        addModdedMixinCFG("mixins.stellar_core_scalingguis.json",          "scalingguis");
        addModdedMixinCFG("mixins.stellar_core_sync.json",                 "sync");
        addModdedMixinCFG("mixins.stellar_core_sync_techguns.json",        "sync", "techguns");
        addModdedMixinCFG("mixins.stellar_core_tconevo.json",              "tconevo");
        addModdedMixinCFG("mixins.stellar_core_tconstruct.json",           "tconstruct");
        addModdedMixinCFG("mixins.stellar_core_techguns.json",             "techguns");
        addModdedMixinCFG("mixins.stellar_core_theoneprobe.json",          "theoneprobe");
        addModdedMixinCFG("mixins.stellar_core_thermaldynamics.json",      "thermaldynamics");
        addModdedMixinCFG("mixins.stellar_core_thermalexpansion.json",     "thermalexpansion", () -> StellarCoreConfig.BUG_FIXES.thermalExpansion.containerSatchelFilter);
        addModdedMixinCFG("mixins.stellar_core_vintagefix.json",           "vintagefix");
    }

    @Override
    public List<String> getMixinConfigs() {
        return new ArrayList<>(MIXIN_CONFIGS.keySet());
    }

    @Override
    public boolean shouldMixinConfigQueue(final String mixinConfig) {
        BooleanSupplier supplier = MIXIN_CONFIGS.get(mixinConfig);
        if (supplier == null) {
            StellarCoreEarlyMixinLoader.LOG.warn(StellarCoreEarlyMixinLoader.LOG_PREFIX + "Mixin config {} is not found in config map! It will never be loaded.", mixinConfig);
            return false;
        }
        boolean shouldLoad = supplier.getAsBoolean();
        if (!shouldLoad) {
            StellarCoreEarlyMixinLoader.LOG.info(StellarCoreEarlyMixinLoader.LOG_PREFIX + "Mixin config {} is disabled by config or mod is not loaded.", mixinConfig);
        }
        return shouldLoad;
    }

    private static boolean modLoaded(final String modID) {
        return Loader.isModLoaded(modID);
    }

    private static void addModdedMixinCFG(final String mixinConfig, final String modID) {
        MIXIN_CONFIGS.put(mixinConfig, () -> modLoaded(modID));
    }

    private static void addModdedMixinCFG(final String mixinConfig, final String modID, final BooleanSupplier condition) {
        MIXIN_CONFIGS.put(mixinConfig, () -> modLoaded(modID) && condition.getAsBoolean());
    }

    private static void addModdedMixinCFG(final String mixinConfig, final String[] modIDs, final BooleanSupplier condition) {
        MIXIN_CONFIGS.put(mixinConfig, () -> Arrays.stream(modIDs).allMatch(Loader::isModLoaded) && condition.getAsBoolean());
    }

    private static void addModdedMixinCFG(final String mixinConfig, final String modID, final String... modIDs) {
        MIXIN_CONFIGS.put(mixinConfig, () -> modLoaded(modID) && Arrays.stream(modIDs).allMatch(Loader::isModLoaded));
    }

    private static void addMixinCFG(final String mixinConfig) {
        MIXIN_CONFIGS.put(mixinConfig, () -> true);
    }

    private static void addMixinCFG(final String mixinConfig, final BooleanSupplier conditions) {
        MIXIN_CONFIGS.put(mixinConfig, conditions);
    }
}
