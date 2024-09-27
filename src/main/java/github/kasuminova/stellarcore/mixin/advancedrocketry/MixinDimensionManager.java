package github.kasuminova.stellarcore.mixin.advancedrocketry;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zmaster587.advancedRocketry.dimension.DimensionManager;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = DimensionManager.class, remap = false)
public class MixinDimensionManager {

    @Redirect(method = "createAndLoadDimensions", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;exitJava(IZ)V"))
    private void redirectCreateAndLoadDimensions(final FMLCommonHandler instance, final int i, final boolean exitCode) {
        if (!StellarCoreConfig.BUG_FIXES.advancedRocketry.dimensionManager) {
            FMLCommonHandler.instance().exitJava(i, exitCode);
            return;
        }

        StellarLog.LOG.warn("************************************************************************************************************************");
        StellarLog.LOG.warn("* StellarCore has stopped the crash caused by AR's failure to read planetDefs.xml! File generation will now be restarted.");
        StellarLog.LOG.warn("************************************************************************************************************************");
    }

}
