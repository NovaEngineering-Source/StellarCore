package github.kasuminova.stellarcore.common.integration.censoredasm;

import github.kasuminova.stellarcore.common.mod.Mods;
import github.kasuminova.stellarcore.common.util.StellarLog;
import mirror.normalasm.config.NormalConfig;
import zone.rong.loliasm.config.LoliConfig;

/**
 * I hate all this messy politics.
 */
@SuppressWarnings("LoggingSimilarMessage")
public class CensoredASMCompat {

    public static boolean checkDelayItemStackCapInitEnabled() {
        if (Mods.CENSORED_ASM.loaded()) {
            if (LoliConfig.instance.delayItemStackCapabilityInit) {
                StellarLog.LOG.warn("***************************************************************************");
                StellarLog.LOG.warn("*");
                StellarLog.LOG.warn("* CensoredASM option `delayItemStackCapabilityInit` is enabled.");
                StellarLog.LOG.warn("* This is not compatible with StellarCore's `AsyncItemStackCapabilityInit`.");
                StellarLog.LOG.warn("*");
                StellarLog.LOG.warn("* StellarCore now will automatic disable `AsyncItemStackCapabilityInit`.");
                StellarLog.LOG.warn("*");
                StellarLog.LOG.warn("***************************************************************************");
                return true;
            }
        }
        if (Mods.FERMIUM_OR_BLAHAJ_ASM.loaded()) {
            if (NormalConfig.instance.delayItemStackCapabilityInit) {
                StellarLog.LOG.warn("**********************************************************************************");
                StellarLog.LOG.warn("*");
                StellarLog.LOG.warn("* NormalASM/FermiumASM/BlahajASM option `delayItemStackCapabilityInit` is enabled.");
                StellarLog.LOG.warn("* This is not compatible with StellarCore's `AsyncItemStackCapabilityInit`.");
                StellarLog.LOG.warn("*");
                StellarLog.LOG.warn("* StellarCore now will automatic disable `AsyncItemStackCapabilityInit`.");
                StellarLog.LOG.warn("*");
                StellarLog.LOG.warn("**********************************************************************************");
                return true;
            }
        }
        return false;
    }

    public static boolean checkNBTMapModified() {
        if (Mods.CENSORED_ASM.loaded()) {
            if (LoliConfig.instance.optimizeNBTTagCompoundBackingMap || LoliConfig.instance.nbtBackingMapStringCanonicalization) {
                StellarLog.LOG.warn("************************************************************************************************************");
                StellarLog.LOG.warn("*");
                StellarLog.LOG.warn("* CensoredASM option `optimizeNBTTagCompoundBackingMap` or `nbtBackingMapStringCanonicalization` is enabled.");
                StellarLog.LOG.warn("* This is not compatible with StellarCore's `NBTTagCompoundMapImprovements`.");
                StellarLog.LOG.warn("*");
                StellarLog.LOG.warn("* StellarCore now will automatic disable `NBTTagCompoundMapImprovements`.");
                StellarLog.LOG.warn("*");
                StellarLog.LOG.warn("************************************************************************************************************");
                return true;
            }
        }
        if (Mods.FERMIUM_OR_BLAHAJ_ASM.loaded()) {
            if (NormalConfig.instance.optimizeNBTTagCompoundBackingMap || NormalConfig.instance.nbtBackingMapStringCanonicalization) {
                StellarLog.LOG.warn("*******************************************************************************************************************************");
                StellarLog.LOG.warn("*");
                StellarLog.LOG.warn("* NormalASM/FermiumASM/BlahajASM option `optimizeNBTTagCompoundBackingMap` or `nbtBackingMapStringCanonicalization` is enabled.");
                StellarLog.LOG.warn("* This is not compatible with StellarCore's `NBTTagCompoundMapImprovements`.");
                StellarLog.LOG.warn("*");
                StellarLog.LOG.warn("* StellarCore now will automatic disable `NBTTagCompoundMapImprovements`.");
                StellarLog.LOG.warn("*");
                StellarLog.LOG.warn("*******************************************************************************************************************************");
                return true;
            }
        }
        return false;
    }

    public static boolean isPresent() {
        return Mods.CENSORED_ASM.loaded() || Mods.FERMIUM_OR_BLAHAJ_ASM.loaded();
    }

}
