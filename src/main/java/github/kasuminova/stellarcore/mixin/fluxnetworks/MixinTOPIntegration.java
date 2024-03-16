package github.kasuminova.stellarcore.mixin.fluxnetworks;

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
        return stellar_core$getTOPTransKey(instance);
    }

    @SuppressWarnings("MethodMayBeStatic")
    @Redirect(method = "addProbeInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lsonar/fluxnetworks/common/core/FluxUtils;getTransferInfo(Lsonar/fluxnetworks/api/network/ConnectionType;Lsonar/fluxnetworks/api/utils/EnergyType;J)Ljava/lang/String;",
                    remap = false),
            remap = false)
    public String redirectAddProbeInfoGetTransferInfo(final ConnectionType connectionType, final EnergyType energyType, final long change) {
        return stellar_core$getTransferInfo(connectionType, energyType, change);
    }

    @Unique
    private static String stellar_core$getTransferInfo(ConnectionType type, EnergyType energyType, long change) {
        if (type.isPlug()) {
            String formatted = FluxUtils.format(change, FluxUtils.TypeNumberFormat.COMMAS, energyType, true);
            return change == 0L ? stellar_core$getTOPTransKey(FluxTranslate.INPUT) + ": " + TextFormatting.GOLD + formatted : stellar_core$getTOPTransKey(FluxTranslate.INPUT) + ": " + TextFormatting.GREEN + "+" + formatted;
        }
        if (!type.isPoint() && !type.isController()) {
            if (type != ConnectionType.STORAGE) {
                return "";
            }
            if (change == 0L) {
                return stellar_core$getTOPTransKey(FluxTranslate.CHANGE) + ": " + TextFormatting.GOLD + change + energyType.getUsageSuffix();
            }
            if (change > 0L) {
                return stellar_core$getTOPTransKey(FluxTranslate.CHANGE) + ": " + TextFormatting.RED + "-" + FluxUtils.format(change, FluxUtils.TypeNumberFormat.COMMAS, energyType, true);
            }
            return stellar_core$getTOPTransKey(FluxTranslate.CHANGE) + ": " + TextFormatting.GREEN + "+" + FluxUtils.format(-change, FluxUtils.TypeNumberFormat.COMMAS, energyType, true);
        }
        String formatted = FluxUtils.format(-change, FluxUtils.TypeNumberFormat.COMMAS, energyType, true);
        if (change == 0L) {
            return stellar_core$getTOPTransKey(FluxTranslate.OUTPUT) + ": " + TextFormatting.GOLD + formatted;
        }
        return stellar_core$getTOPTransKey(FluxTranslate.OUTPUT) + ": " + TextFormatting.RED + "-" + formatted;
    }

    @Unique
    private static String stellar_core$getTOPTransKey(Translation translation) {
        return "{*" + translation.key + "*}";
    }

}