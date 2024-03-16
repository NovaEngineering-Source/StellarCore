package github.kasuminova.stellarcore.mixin.fluxnetworks;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sonar.fluxnetworks.api.network.ConnectionType;
import sonar.fluxnetworks.api.translate.FluxTranslate;
import sonar.fluxnetworks.api.translate.Translation;
import sonar.fluxnetworks.api.utils.EnergyType;
import sonar.fluxnetworks.common.core.FluxUtils;
import sonar.fluxnetworks.common.integration.TOPIntegration;

@Mixin(TOPIntegration.FluxConnectorInfoProvider.class)
public class MixinTOPIntegration {

    @SuppressWarnings("MethodMayBeStatic")
    @Redirect(method = "addProbeInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lsonar/fluxnetworks/api/translate/Translation;t()Ljava/lang/String;",
                    remap = false),
            remap = false)
    public String redirectAddProbeInfoTrans(final Translation instance) {
        if (!StellarCoreConfig.BUG_FIXES.fluxNetworks.fixTop) {
            return instance.t();
        }
        return stellar_core$getTOPTransKey(instance);
    }

    @SuppressWarnings("MethodMayBeStatic")
    @Redirect(method = "addProbeInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lsonar/fluxnetworks/common/core/FluxUtils;getTransferInfo(Lsonar/fluxnetworks/api/network/ConnectionType;Lsonar/fluxnetworks/api/utils/EnergyType;J)Ljava/lang/String;",
                    remap = false),
            remap = false)
    public String redirectAddProbeInfoGetTransferInfo(final ConnectionType connectionType, final EnergyType energyType, final long charge) {
        if (!StellarCoreConfig.BUG_FIXES.fluxNetworks.fixTop) {
            return FluxUtils.getTransferInfo(connectionType, energyType, charge);
        }
        return stellar_core$getTransferInfo(connectionType, energyType, charge);
    }

    @Unique
    private static String stellar_core$getTransferInfo(ConnectionType type, EnergyType energyType, long charge) {
        if (type.isPlug()) {
            String formatted = FluxUtils.format(charge, FluxUtils.TypeNumberFormat.COMMAS, energyType, true);
            return charge == 0L ? stellar_core$getTOPTransKey(FluxTranslate.INPUT) + ": " + TextFormatting.GOLD + formatted : stellar_core$getTOPTransKey(FluxTranslate.INPUT) + ": " + TextFormatting.GREEN + "+" + formatted;
        }
        if (!type.isPoint() && !type.isController()) {
            if (type != ConnectionType.STORAGE) {
                return "";
            }
            if (charge == 0L) {
                return stellar_core$getTOPTransKey(FluxTranslate.CHANGE) + ": " + TextFormatting.GOLD + charge + energyType.getUsageSuffix();
            }
            if (charge > 0L) {
                return stellar_core$getTOPTransKey(FluxTranslate.CHANGE) + ": " + TextFormatting.RED + "-" + FluxUtils.format(charge, FluxUtils.TypeNumberFormat.COMMAS, energyType, true);
            }
            return stellar_core$getTOPTransKey(FluxTranslate.CHANGE) + ": " + TextFormatting.GREEN + "+" + FluxUtils.format(-charge, FluxUtils.TypeNumberFormat.COMMAS, energyType, true);
        }
        String formatted = FluxUtils.format(-charge, FluxUtils.TypeNumberFormat.COMMAS, energyType, true);
        if (charge == 0L) {
            return stellar_core$getTOPTransKey(FluxTranslate.OUTPUT) + ": " + TextFormatting.GOLD + formatted;
        }
        return stellar_core$getTOPTransKey(FluxTranslate.OUTPUT) + ": " + TextFormatting.RED + "-" + formatted;
    }

    @Unique
    private static String stellar_core$getTOPTransKey(Translation translation) {
        return "{*" + translation.key + "*}";
    }

}