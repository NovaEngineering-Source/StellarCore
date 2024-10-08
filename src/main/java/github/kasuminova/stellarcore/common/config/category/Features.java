package github.kasuminova.stellarcore.common.config.category;

import net.minecraftforge.common.config.Config;

public class Features {

    @Config.Comment({
            "(Client) Allows you to modify the title of the game, highest priority, ",
            "supports earlier versions such as CleanroomLoader 3029 (May do the same thing with other mods)."
    })
    @Config.Name("EnableCustomGameTitle")
    public boolean enableTitle = false;

    @Config.Comment("Does the CustomGameTitle use Hitokoto API to get random messages? (Chinese Only)")
    @Config.Name("TitleUseHitokotoAPI")
    public boolean hitokoto = false;

    @Config.Comment("The title.")
    @Config.Name("CustomGameTitle")
    public String title = "Minecraft 1.12.2";

    @Config.Name("Vanilla")
    public final Vanilla vanilla = new Vanilla();

    @Config.Name("FontScale")
    public final FontScale fontScale = new FontScale();

    @Config.Name("AstralSorcery")
    public final AstralSorcery astralSorcery = new AstralSorcery();

    @Config.Name("BetterChat")
    public final BetterChat betterChat = new BetterChat();

    @Config.Name("Botania")
    public final Botania botania = new Botania();

    @Config.Name("EBWizardry")
    public final EBWizardry ebwizardry = new EBWizardry();

    @Config.Name("EnderIOConduits")
    public final EnderIOConduits enderIOConduits = new EnderIOConduits();

    @Config.Name("FluxNetworks")
    public final FluxNetworks fluxNetworks = new FluxNetworks();

    @Config.Name("IC2")
    public final IC2 ic2 = new IC2();

    @Config.Name("LazyAE2")
    public final LazyAE2 lazyAE2 = new LazyAE2();

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

    public static class Vanilla {

        @Config.Comment("(Server) Allows CriterionProgress to be serialized in multiple threads.")
        @Config.RequiresMcRestart
        @Config.Name("AsyncAdvancementSerialize")
        public boolean asyncAdvancementSerialize = true;

        @Config.Comment({
                "An extra feature that stops the model loader from printing errors, neat log, no?",
                "May have implications for Debug, cannot prevent errors in the output of custom loaders. only available if ParallelModelLoader is enabled."
        })
        @Config.RequiresMcRestart
        @Config.Name("ShutUpModelLoader")
        public boolean shutUpModelLoader = false;

        @Config.Comment("(Client Only) Listening to clients loading/unloading new worlds, disabling this option will cause some features on memory leak fixing to fail.")
        @Config.RequiresMcRestart
        @Config.Name("HandleClientWorldLoad")
        public boolean handleClientWorldLoad = true;

        @Config.Comment({
                "(Server) Define which entities will be forced to be updated.",
                "The update to stop when there are no players near the entity, which may cause some projectiles to pile up.",
                "This feature allows certain entities to be forced to be updated.",
                "Note: Entity classes must be explicitly defined and their superclasses cannot be retrieved, this is for performance reasons."
        })
        @Config.RequiresMcRestart
        @Config.Name("ForceUpdateEntityClasses")
        public String[] forceUpdateEntityClasses = {
                "cofh.redstonearsenal.entity.projectile.EntityArrowFlux",
                "com.brandon3055.draconicevolution.entity.EntityCustomArrow",
                "hellfirepvp.astralsorcery.common.entities.EntityFlare",
                "hellfirepvp.astralsorcery.common.entities.EntityLiquidSpark",
                "mekanism.weapons.common.entity.EntityMekaArrow", // MEKCEu
                "net.lrsoft.mets.entity.EntityGunBullet",
                "net.lrsoft.mets.entity.EntityHyperGunBullet",
                "net.lrsoft.mets.entity.EntityPlasmaBullet",
                "net.lrsoft.mets.entity.EntityTachyonBullet",
                "net.minecraft.entity.projectile.EntitySpectralArrow",
                "thundr.redstonerepository.entity.projectile.EntityArrowGelid",
                "xyz.phanta.tconevo.entity.EntityMagicMissile",
        };

        @Config.Comment({
                "Completely remove something from the Forge registry, use at your own risk.",
                "Usage: `minecraft:dirt`, `modid:something`"
        })
        @Config.RequiresMcRestart
        @Config.Name("ForgeRegistryRemoveList")
        public String[] forgeRegistryRemoveList = {};

    }

    public static class FontScale {

        @Config.Comment("(Client) Allows you to modify the specific scaling of small fonts in the AE2 GUI.")
        @Config.RangeDouble(min = 0.25F, max = 1.0F)
        @Config.Name("AppliedEnergetics2")
        public float ae2 = 0.5F;

        @Config.Comment("(Client) Allows you to modify the specific scaling of small fonts in the EnderUtilities GUI.")
        @Config.RangeDouble(min = 0.25F, max = 1.0F)
        @Config.Name("EnderUtilities")
        public float enderUtilities = 0.5F;

    }

    public static class AstralSorcery {

        @Config.Comment("Disables AstralSorcery's ChainMining perk, make that doesn't work.")
        @Config.Name("DisableChainMining")
        public boolean disableChainMining = false;

    }

    public static class BetterChat {

        @Config.Comment("(Client) Message compat (probably does the same thing as UniversalTweaks, but the difference is that this is a special compatibility with the BetterChat mod).")
        @Config.Name("EnableMessageCompat")
        public boolean messageCompat = false;

    }

    public static class Botania {

        @Config.Comment("As the name suggests, use at your own risk.")
        @Config.Name("DisableCosmeticRecipe")
        public boolean disableCosmeticRecipe = false;

    }

    public static class EBWizardry {

        @Config.Comment("Prevents the WizardSpell loot from logging to the server console when it's casted.")
        @Config.Name("PreventWizardSpellLogSpam")
        public boolean preventWizardSpellLogSpam = false;

    }

    public static class EnderIOConduits {

        @Config.Comment("If you're really tired of all this useless logging, set it to true (filter only the no side effects section).")
        @Config.Name("PrevEnderLiquidConduitNetworkLogSpam")
        public boolean prevEnderLiquidConduitLogSpam = true;

    }

    public static class FluxNetworks {

        @Config.Comment("(Server) Make FluxNetworks to generate a random int uid for each network, instead of using the self-incrementing ID.")
        @Config.Name("RandomNetworkUniqueID")
        public boolean randomNetworkUniqueID = false;

    }

    public static class IC2 {

        @Config.Comment({
                "A highly intrusive feature that makes the IC2 and most of its Addon mod's power items no longer use the endurance value to",
                "display power, but instead use a special display, a feature that disables the endurance value and helps automate the crafting."
        })
        @Config.Name("ElectricItemNonDurability")
        public boolean electricItemNonDurability = false;

    }

    public static class LazyAE2 {

        @Config.Comment("The Level Maintainer request synthesis will always be made to the set value, not just to the critical value.")
        @Config.Name("LevelMaintainerRequestCountImprovements")
        public boolean levelMaintainerRequest = false;

    }

    public static class LegendaryTooltips {

        @Config.Comment("(Client) As the name suggests, enable it only when necessary.")
        @Config.Name("DisableTitleWrap")
        public boolean tooltipDecor = false;

    }

    public static class Mekanism {

        @Config.Comment({
                "Allows TheOneProbe to show that Mekanism's machines exceed 2147483647 units of energy.",
                "MEKCEu already includes this feature, so installing MEKCEu will automatically disable it."
        })
        @Config.RequiresMcRestart
        @Config.Name("TOPSupport")
        public boolean topSupport = true;

        @Config.Comment({
                "Allows Mekanism's machines to transmit more than 2147483647 units of energy through FluxNetworks.",
                "MEKCEu already includes this feature, so installing MEKCEu will automatically disable it."
        })
        @Config.Name("FluxNetworksSupport")
        public boolean fluxNetworksSupport = true;

    }

    public static class NuclearCraftOverhauled {

        @Config.Comment("Completely disable NuclearCraft: Overhauled's radiation system if you really don't want to see them in every item's NBT, haha.")
        @Config.Name("DisableRadiationCapability")
        public boolean removeRadiationCapabilityHandler = false;

    }

    public static class RGBChat {

        @Config.Comment("(Client) Complete rewrite of RGBChat's font renderer to optimize performance and fix crashes.")
        @Config.Name("TrueRGBSimpleRendererImprovements")
        public boolean cachedRGBFontRenderer = true;

    }

    public static class Techguns {

        @Config.Comment("Safe mode is used by default for every player.")
        @Config.Name("ForceSecurityMode")
        public boolean forceSecurityMode = true;

        @Config.Comment("Are bullets treated as projectiles (affecting damage determination)?")
        @Config.Name("BulletIsProjectile")
        public boolean tgDamageSource = false;

    }

    public static class MoreElectricTools {

        @Config.Comment("Disable the Efficient enchantment, if you think this enchantment will appear on any item it's just too bad.")
        @Config.Name("RemoveEfficientEnergyCostEnchantment")
        public boolean disableEfficientEnergyCost = false;

    }

}
