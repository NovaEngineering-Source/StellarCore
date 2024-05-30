package github.kasuminova.stellarcore.common.mod;

import net.minecraftforge.fml.common.Loader;

public enum Mods {

    FTBLIB("ftblib"),
    FTBQ(  "ftbquests"),
    MEK(   "mekanism"),
    MEKCEU("mekanism") {
        @Override
        public boolean loaded() {
            if (!MEK.loaded()) {
                return false;
            }
            if (initialized) {
                return loaded;
            }

            try {
                Class.forName("mekanism.common.config.MEKCEConfig");
                initialized = true;
                return loaded = true;
            } catch (Throwable e) {
                return loaded = false;
            }
        }
    },

    ;

    protected final String modID;
    protected boolean loaded = false;
    protected boolean initialized = false;

    Mods(final String modID) {
        this.modID = modID;
    }

    public boolean loaded() {
        if (!initialized) {
            loaded = Loader.isModLoaded(modID);
            initialized = true;
        }
        return loaded;
    }

}
