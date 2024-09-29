package github.kasuminova.stellarcore.mixin.fluxnetworks;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.IStellarFluxNetwork;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sonar.fluxnetworks.api.network.IFluxNetwork;
import sonar.fluxnetworks.common.CommonProxy;
import sonar.fluxnetworks.common.connection.FluxNetworkCache;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = CommonProxy.class, remap = false)
public class MixinCommonProxy {

    @Inject(method = "onServerTick", at = @At("HEAD"))
    private void injectOnServerTick(final TickEvent.ServerTickEvent event, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.fluxNetworks.parallelNetworkCalculation) {
            return;
        }
        if (event.phase == TickEvent.Phase.END) {
            Collection<IFluxNetwork> networks = FluxNetworkCache.instance.getAllNetworks();
            List<Runnable> runnableList = new ObjectArrayList<>(networks.size() + 1);
            networks.stream().filter(IStellarFluxNetwork.class::isInstance)
                    .map(IStellarFluxNetwork.class::cast)
                    .map(IStellarFluxNetwork::getCycleStartRunnable)
                    .forEach(runnableList::add);
            runnableList.parallelStream().forEach(Runnable::run);
        }
    }

}
