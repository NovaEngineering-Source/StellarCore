package github.kasuminova.stellarcore.mixin.minecraft.forge;

import github.kasuminova.stellarcore.client.profiler.PacketProfiler;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleIndexedCodec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SimpleIndexedCodec.class)
public class MixinSimpleIndexedCodec {

    @Redirect(method = "decodeInto(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Lnet/minecraftforge/fml/common/network/simpleimpl/IMessage;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fml/common/network/simpleimpl/IMessage;fromBytes(Lio/netty/buffer/ByteBuf;)V",
                    remap = false),
            remap = false
    )
    private void onDecodePre(final IMessage message, final ByteBuf byteBuf) {
        final int prevIndex = byteBuf.readerIndex();

        message.fromBytes(byteBuf);

        PacketProfiler.onPacketReceived(message, byteBuf.readerIndex() - prevIndex);
    }

}
