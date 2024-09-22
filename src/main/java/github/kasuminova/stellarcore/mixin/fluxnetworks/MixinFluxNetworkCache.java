package github.kasuminova.stellarcore.mixin.fluxnetworks;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.RandomUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sonar.fluxnetworks.api.network.IFluxNetwork;
import sonar.fluxnetworks.common.connection.FluxNetworkCache;
import sonar.fluxnetworks.common.data.FluxNetworkData;

import java.util.Map;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = FluxNetworkCache.class, remap = false)
public class MixinFluxNetworkCache {

    @Inject(method = "getUniqueID", at = @At("HEAD"), cancellable = true)
    private void injectGetUniqueID(final CallbackInfoReturnable<Integer> cir) {
        if (!StellarCoreConfig.FEATURES.fluxNetworks.randomNetworkUniqueID) {
            return;
        }

        Map<Integer, IFluxNetwork> networks = FluxNetworkData.get().networks;
        int newID = RandomUtils.nextInt();
        while (newID == -1 || networks.containsKey(newID)) {
            newID = RandomUtils.nextInt();
        }

        cir.setReturnValue(newID);
    }

}
