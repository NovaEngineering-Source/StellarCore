package github.kasuminova.stellarcore.common.mod;

import net.minecraftforge.fml.common.Loader;

public enum Mods {

    FTBLIB(               "ftblib"),
    FTBQ(                 "ftbquests"),
    MEK(                  "mekanism"),
    REPLAY(               "replaymod"),
    VINTAGE_FIX(          "vintagefix"),
    RGB_CHAT(             "jianghun"), // ?
    TLM(                  "touhou_little_maid"),
    MM(                   "modularmachinery"),
    EBWIZARDRY(           "ebwizardry"),
    LIB_NINE(             "libnine"),
    MMCE(                 "modularmachinery",  "github.kasuminova.mmce.mixin.MMCEEarlyMixinLoader"),
    CENSORED_ASM(         "loliasm",           "zone.rong.loliasm.core.LoliLoadingPlugin"),
    FERMIUM_OR_BLAHAJ_ASM("normalasm",         "mirror.normalasm.core.NormalLoadingPlugin"),
    TICK_CENTRAL(         "tickcentral",       "com.github.terminatornl.tickcentral.TickCentral"),
    MEKCEU(               "mekanism",          "mekanism.common.concurrent.TaskExecutor") {
        @Override
        public boolean loaded() {
            if (!MEK.loaded()) {
                return false;
            }
            return super.loaded();
        }
    },
    NCO(                  "nuclearcraft") {
        @Override
        public boolean loaded() {
            if (!super.loaded()) {
                return false;
            }
            return this.loaded = Loader.instance().getIndexedModList().get(modID).getVersion().contains("2o");
        }
    }
    ;

    final String modID;
    final String requiredClass;
    boolean loaded = false;
    boolean initialized = false;

    Mods(final String modID) {
        this.modID = modID;
        this.requiredClass = null;
    }

    Mods(final String modID, final String requiredClass) {
        this.modID = modID;
        this.requiredClass = requiredClass;
    }

    public boolean loaded() {
        if (initialized) {
            return loaded;
        }

        initialized = true;

        if (requiredClass != null) {
            try {
                Class.forName(requiredClass);
                return loaded = true;
            } catch (Throwable e) {
                return loaded = false;
            }
        }
        return loaded = Loader.isModLoaded(modID);
    }

}
