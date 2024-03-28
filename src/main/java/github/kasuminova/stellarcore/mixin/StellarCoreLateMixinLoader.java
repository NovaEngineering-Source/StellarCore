package github.kasuminova.stellarcore.mixin;

import github.kasuminova.stellarcore.client.hitokoto.HitokotoAPI;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

@SuppressWarnings("unused")
public class StellarCoreLateMixinLoader implements ILateMixinLoader {

    private static final Map<String, BooleanSupplier> MIXIN_CONFIGS = new LinkedHashMap<>();

    static {
        addModdedMixinCFG("mixins.stellar_core_advancedrocketry.json",     "advancedrocketry");
        addModdedMixinCFG("mixins.stellar_core_ae.json",                   "appliedenergistics2");
        addModdedMixinCFG("mixins.stellar_core_armourers_workshop.json",   "armourers_workshop");
        addModdedMixinCFG("mixins.stellar_core_astralsorcery.json",        "astralsorcery");
        addModdedMixinCFG("mixins.stellar_core_avaritia.json",             "avaritia");
        addModdedMixinCFG("mixins.stellar_core_betterchat.json",           "betterchat");
        addModdedMixinCFG("mixins.stellar_core_biomesoplenty.json",        "biomesoplenty");
        addModdedMixinCFG("mixins.stellar_core_bloodmagic.json",           "bloodmagic");
        addModdedMixinCFG("mixins.stellar_core_botania.json",              "botania");
        addModdedMixinCFG("mixins.stellar_core_cfm.json",                  "cfm", () -> StellarCoreConfig.BUG_FIXES.mrCrayfishFurniture.imageCache);
        addModdedMixinCFG("mixins.stellar_core_chisel.json",               "chisel");
        addModdedMixinCFG("mixins.stellar_core_cucumber.json",             "cucumber");
        addModdedMixinCFG("mixins.stellar_core_draconicevolution.json",    "draconicevolution");
        addModdedMixinCFG("mixins.stellar_core_endercore.json",            "endercore");
        addModdedMixinCFG("mixins.stellar_core_enderio.json",              "enderio");
        addModdedMixinCFG("mixins.stellar_core_enderioconduits.json",      "enderio", "enderioconduits");
        addModdedMixinCFG("mixins.stellar_core_extrabotany.json",          "extrabotany");
        addModdedMixinCFG("mixins.stellar_core_fluxnetworks.json",         "fluxnetworks");
        addModdedMixinCFG("mixins.stellar_core_ic2.json",                  "ic2");
        addModdedMixinCFG("mixins.stellar_core_igi.json",                  "ingameinfoxml");
        addModdedMixinCFG("mixins.stellar_core_immersiveengineering.json", "immersiveengineering");
        addModdedMixinCFG("mixins.stellar_core_legendarytooltips.json",    "legendarytooltips");
        addModdedMixinCFG("mixins.stellar_core_mek_top.json",        new String[]{"mekanism", "theoneprobe"}, () -> StellarCoreConfig.FEATURES.mekanism.topSupport);
        addModdedMixinCFG("mixins.stellar_core_mekanism.json",             "mekanism");
        addModdedMixinCFG("mixins.stellar_core_mets.json",                 "mets");
        addModdedMixinCFG("mixins.stellar_core_nco.json",                  "nuclearcraft");
        addModdedMixinCFG("mixins.stellar_core_rgb_chat.json",             "jianghun");
        addModdedMixinCFG("mixins.stellar_core_scalingguis.json",          "scalingguis");
        addModdedMixinCFG("mixins.stellar_core_sync.json",                 "sync");
        addModdedMixinCFG("mixins.stellar_core_sync_techguns.json",        "sync", "techguns");
        addModdedMixinCFG("mixins.stellar_core_techguns.json",             "techguns");
        addModdedMixinCFG("mixins.stellar_core_theoneprobe.json",          "theoneprobe");
        addModdedMixinCFG("mixins.stellar_core_thermaldynamics.json",      "thermaldynamics");
    }

    static {
        if (StellarCoreConfig.FEATURES.hitokoto) {
            new Thread(() -> {
                Thread.currentThread().setName("Stellar Core Hitokoto Initializer");
                String hitokoto = HitokotoAPI.getRandomHitokoto();
                if (hitokoto == null || hitokoto.isEmpty()) {
                    return;
                }
                StellarCoreEarlyMixinLoader.LOG.info(StellarCoreEarlyMixinLoader.LOG_PREFIX + hitokoto);
            }).start();
        }
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
        return supplier.getAsBoolean();
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
