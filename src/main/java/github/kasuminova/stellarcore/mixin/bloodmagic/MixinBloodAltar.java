package github.kasuminova.stellarcore.mixin.bloodmagic;

import WayofTime.bloodmagic.altar.BloodAltar;
import WayofTime.bloodmagic.tile.TileAltar;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BloodAltar.class)
public class MixinBloodAltar {

    @Shadow(remap = false) private TileAltar tileAltar;

    private static void sendUpdatePacketToNearPlayers(final TileAltar altar, final World world, final BlockPos blockPos) {
        MinecraftServer server = world.getMinecraftServer();
        if (server == null) {
            return;
        }

        server.addScheduledTask(() -> {
            if (altar.isInvalid()) {
                return;
            }
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                if (player.world != world) {
                    continue;
                }
                double distance = player.getPosition().getDistance(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                if (distance <= 16) {
                    player.connection.sendPacket(altar.getUpdatePacket());
                }
            }
        });
    }

    @Redirect(method = "startCycle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;notifyBlockUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/block/state/IBlockState;I)V",
                    remap = true),
            remap = false)
    public void onStartCycleNotifyUpdate(final World world, final BlockPos blockPos, final IBlockState pos, final IBlockState oldState, final int newState) {
        if (!StellarCoreConfig.PERFORMANCE.bloodMagic.bloodAltar) {
            world.notifyBlockUpdate(blockPos, pos, oldState, newState);
            return;
        }
        sendUpdatePacketToNearPlayers(tileAltar, world, blockPos);
    }

    @Redirect(method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;notifyBlockUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/block/state/IBlockState;I)V",
                    remap = true),
            remap = false)
    public void onUpdateNotifyUpdate(final World world, final BlockPos blockPos, final IBlockState pos, final IBlockState oldState, final int newState) {
        if (!StellarCoreConfig.PERFORMANCE.bloodMagic.bloodAltar) {
            world.notifyBlockUpdate(blockPos, pos, oldState, newState);
            return;
        }
        sendUpdatePacketToNearPlayers(tileAltar, world, blockPos);
    }

    @Redirect(method = "updateAltar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;notifyBlockUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/block/state/IBlockState;I)V",
                    remap = true),
            remap = false)
    public void onUpdateAltarCycleNotifyUpdate(final World world, final BlockPos blockPos, final IBlockState pos, final IBlockState oldState, final int newState) {
        if (!StellarCoreConfig.PERFORMANCE.bloodMagic.bloodAltar) {
            world.notifyBlockUpdate(blockPos, pos, oldState, newState);
            return;
        }
        sendUpdatePacketToNearPlayers(tileAltar, world, blockPos);
    }

}
