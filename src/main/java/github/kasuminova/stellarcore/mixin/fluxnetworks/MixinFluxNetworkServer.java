package github.kasuminova.stellarcore.mixin.fluxnetworks;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.IStellarFluxNetwork;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sonar.fluxnetworks.api.network.*;
import sonar.fluxnetworks.api.tiles.IFluxConnector;
import sonar.fluxnetworks.api.tiles.IFluxPlug;
import sonar.fluxnetworks.api.tiles.IFluxPoint;
import sonar.fluxnetworks.common.connection.FluxNetworkBase;
import sonar.fluxnetworks.common.connection.FluxNetworkServer;
import sonar.fluxnetworks.common.connection.PriorityGroup;
import sonar.fluxnetworks.common.connection.TransferIterator;
import sonar.fluxnetworks.common.connection.transfer.FluxControllerHandler;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("SynchronizeOnNonFinalField")
@Mixin(value = FluxNetworkServer.class, remap = false)
public abstract class MixinFluxNetworkServer extends FluxNetworkBase implements IFluxNetwork, IStellarFluxNetwork {

    @Unique
    private static final boolean stellar_core$SHOULD_PARALLEL = Runtime.getRuntime().availableProcessors() > 2;

    @Shadow
    protected abstract void handleConnectionQueue();

    @Shadow
    public long bufferLimiter;

    @Shadow
    @Final
    private List<PriorityGroup<IFluxPoint>> sortedPoints;

    @Shadow
    @Final
    private List<PriorityGroup<IFluxPlug>> sortedPlugs;

    @Shadow
    @Final
    private TransferIterator<IFluxPlug> plugTransferIterator;

    @Shadow
    @Final
    private TransferIterator<IFluxPoint> pointTransferIterator;

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public Runnable getCycleStartRunnable() {
        handleConnectionQueue();
        return () -> {
            List<IFluxConnector> devices = getConnections(FluxLogicType.ANY);
            devices.parallelStream().forEach(device -> {
                ITransferHandler handler = device.getTransferHandler();
                if (handler instanceof FluxControllerHandler) {
                    synchronized (FluxControllerHandler.class) {
                        handler.onCycleStart();
                    }
                } else {
                    handler.onCycleStart();
                }
            });
        };
    }

    /**
     * @author Kasumi_Nova
     * @reason Parallel Execution
     */
    @Inject(method = "onEndServerTick", at = @At("HEAD"), cancellable = true)
    public void onEndServerTick(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.fluxNetworks.parallelNetworkCalculation || !stellar_core$SHOULD_PARALLEL) {
            return;
        }
        ci.cancel();

        network_stats.getValue().startProfiling();

        bufferLimiter = 0;

        List<IFluxConnector> devices = getConnections(FluxLogicType.ANY);

        if (!sortedPoints.isEmpty() && !sortedPlugs.isEmpty()) {
            plugTransferIterator.reset(sortedPlugs);
            pointTransferIterator.reset(sortedPoints);
            CYCLE:
            while (pointTransferIterator.hasNext()) {
                while (plugTransferIterator.hasNext()) {
                    IFluxPlug plug = plugTransferIterator.next();
                    IFluxPoint point = pointTransferIterator.next();
                    if (plug.getConnectionType() == point.getConnectionType()) {
                        break CYCLE; // Storage always have the lowest priority, the cycle can be broken here.
                    }
                    // we don't need to simulate this action
                    long operate = plug.getTransferHandler().removeFromBuffer(point.getTransferHandler().getRequest());
                    if (operate > 0) {
                        point.getTransferHandler().addToBuffer(operate);
                        continue CYCLE;
                    } else {
                        // although the plug still need transfer (buffer > 0)
                        // but it reached max transfer limit, so we use next plug
                        plugTransferIterator.incrementFlux();
                    }
                }
                break; // all plugs have been used
            }
        }
        for (IFluxConnector f : devices) {
            f.getTransferHandler().onCycleEnd();
            bufferLimiter += f.getTransferHandler().getRequest();
        }

        network_stats.getValue().stopProfiling();
    }

    @Inject(
            method = "getMemberPermission",
            at = @At(
                    value = "INVOKE",
                    target = "Lsonar/fluxnetworks/api/utils/ICustomValue;getValue()Ljava/lang/Object;",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void injectGetMemberPermission(final EntityPlayer player, final CallbackInfoReturnable<AccessLevel> cir) {
        if (!StellarCoreConfig.BUG_FIXES.fluxNetworks.synchronize) {
            return;
        }

        UUID uuid = EntityPlayer.getUUID(player.getGameProfile());

        synchronized (network_players) {
            for (final NetworkMember member : network_players.getValue()) {
                if (member.getPlayerUUID().equals(uuid)) {
                    cir.setReturnValue(member.getAccessPermission());
                    return;
                }
            }
            cir.setReturnValue(network_security.getValue().isEncrypted() ? AccessLevel.NONE : AccessLevel.USER);
        }
    }

    @Inject(method = "addNewMember", at = @At("HEAD"), cancellable = true)
    private void injectAddNewMember(final String name, final CallbackInfo ci) {
        if (!StellarCoreConfig.BUG_FIXES.fluxNetworks.synchronize) {
            return;
        }

        ci.cancel();

        NetworkMember newMember = NetworkMember.createMemberByUsername(name);
        synchronized (network_players) {
            List<NetworkMember> players = network_players.getValue();

            for (final NetworkMember player : players) {
                if (player.getPlayerUUID().equals(newMember.getPlayerUUID())) {
                    return;
                }
            }

            players.add(newMember);
        }
    }

    @Inject(method = "removeMember", at = @At("HEAD"), cancellable = true)
    private void injectRemoveMember(final UUID uuid, final CallbackInfo ci) {
        if (!StellarCoreConfig.BUG_FIXES.fluxNetworks.synchronize) {
            return;
        }

        ci.cancel();
        synchronized (network_players) {
            network_players.getValue().removeIf(member -> member.getPlayerUUID().equals(uuid) && !member.getAccessPermission().canDelete());
        }
    }

    @Inject(method = "getValidMember", at = @At("HEAD"), cancellable = true)
    private void injectGetValidMember(final UUID player, final CallbackInfoReturnable<Optional<NetworkMember>> cir) {
        if (!StellarCoreConfig.BUG_FIXES.fluxNetworks.synchronize) {
            return;
        }

        synchronized (network_players) {
            cir.setReturnValue(network_players.getValue().stream().filter(f -> f.getPlayerUUID().equals(player)).findFirst());
        }
    }

}
