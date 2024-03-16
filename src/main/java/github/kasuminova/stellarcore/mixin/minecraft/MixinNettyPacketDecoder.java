package github.kasuminova.stellarcore.mixin.minecraft;

import github.kasuminova.stellarcore.client.profiler.PacketProfiler;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NettyPacketDecoder.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinNettyPacketDecoder {

    @Redirect(method = "decode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/Packet;readPacketData(Lnet/minecraft/network/PacketBuffer;)V")
    )
    private void onDecode(final Packet<?> packet, final PacketBuffer packetBuffer) throws Exception {
        final int prevIndex = packetBuffer.readerIndex();

        packet.readPacketData(packetBuffer);

        PacketProfiler.onPacketReceived(packet, packetBuffer.readerIndex() - prevIndex);
    }

}
