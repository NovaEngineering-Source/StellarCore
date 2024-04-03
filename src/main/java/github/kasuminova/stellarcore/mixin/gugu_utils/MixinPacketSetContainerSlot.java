package github.kasuminova.stellarcore.mixin.gugu_utils;

import com.warmthdawn.mod.gugu_utils.network.PacketSetContainerSlot;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(PacketSetContainerSlot.Handler.class)
public class MixinPacketSetContainerSlot {

    /**
     * 发包刷取物品，级别致命。
     */
    @Inject(
            method = "onMessage(Lcom/warmthdawn/mod/gugu_utils/network/PacketSetContainerSlot;Lnet/minecraftforge/fml/common/network/simpleimpl/MessageContext;)Lnet/minecraftforge/fml/common/network/simpleimpl/IMessage;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    public void onMessage(final PacketSetContainerSlot message, final MessageContext ctx, final CallbackInfoReturnable<IMessage> cir) {
        if (!StellarCoreConfig.BUG_FIXES.critical.guguUtilsSetContainerPacket) {
            return;
        }
        cir.setReturnValue(null);
    }

}
