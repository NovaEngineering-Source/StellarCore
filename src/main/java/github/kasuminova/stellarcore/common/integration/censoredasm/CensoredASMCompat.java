package github.kasuminova.stellarcore.common.integration.censoredasm;

import github.kasuminova.stellarcore.common.mod.Mods;
import github.kasuminova.stellarcore.common.util.StellarLog;
import zone.rong.loliasm.config.LoliConfig;

@SuppressWarnings("LoggingSimilarMessage")
public class CensoredASMCompat {

    public static boolean checkDelayItemStackCapInitEnabled() {
        if (Mods.CENSORED_ASM.loaded()) {
            if (LoliConfig.instance.delayItemStackCapabilityInit) {
                StellarLog.LOG.warn("***************************************************************************");
                StellarLog.LOG.warn("* CensoredASM option `delayItemStackCapabilityInit` is enabled.");
                StellarLog.LOG.warn("* This is not compatible with StellarCore's `AsyncItemStackCapabilityInit`.");
                StellarLog.LOG.warn("* StellarCore now will automatic disable `AsyncItemStackCapabilityInit`.");
                StellarLog.LOG.warn("***************************************************************************");
                return true;
            }
        }
        return false;
    }

    public static boolean checkNBTMapModified() {
        if (Mods.CENSORED_ASM.loaded()) {
            if (LoliConfig.instance.optimizeNBTTagCompoundBackingMap || LoliConfig.instance.nbtBackingMapStringCanonicalization) {
                StellarLog.LOG.warn("************************************************************************************************************");
                StellarLog.LOG.warn("* CensoredASM option `optimizeNBTTagCompoundBackingMap` or `nbtBackingMapStringCanonicalization` is enabled.");
                StellarLog.LOG.warn("* This is not compatible with StellarCore's `NBTTagCompoundMapImprovements`.");
                StellarLog.LOG.warn("* StellarCore now will automatic disable `NBTTagCompoundMapImprovements`.");
                StellarLog.LOG.warn("************************************************************************************************************");
                return true;
            }
        }
        return false;
    }

}
