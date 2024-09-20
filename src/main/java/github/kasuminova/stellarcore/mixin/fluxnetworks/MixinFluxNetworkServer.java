package github.kasuminova.stellarcore.mixin.fluxnetworks;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sonar.fluxnetworks.api.network.AccessLevel;
import sonar.fluxnetworks.api.network.NetworkMember;
import sonar.fluxnetworks.common.connection.FluxNetworkBase;
import sonar.fluxnetworks.common.connection.FluxNetworkServer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("SynchronizeOnNonFinalField")
@Mixin(value = FluxNetworkServer.class, remap = false)
public class MixinFluxNetworkServer extends FluxNetworkBase {

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
