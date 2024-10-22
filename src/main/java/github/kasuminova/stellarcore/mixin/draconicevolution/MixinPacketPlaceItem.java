package github.kasuminova.stellarcore.mixin.draconicevolution;

import com.brandon3055.draconicevolution.network.PacketPlaceItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(PacketPlaceItem.Handler.class)
public class MixinPacketPlaceItem {

    @Inject(
            method = "handleMessage(Lcom/brandon3055/draconicevolution/network/PacketPlaceItem;Lnet/minecraftforge/fml/common/network/simpleimpl/MessageContext;)Lnet/minecraftforge/fml/common/network/simpleimpl/IMessage;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcodechicken/lib/raytracer/RayTracer;retrace(Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/util/math/RayTraceResult;"
            ),
            remap = false,
            cancellable = true
    )
    private void injectHandleMessage(final PacketPlaceItem message, final MessageContext ctx, final CallbackInfoReturnable<IMessage> cir) {
        EntityPlayer player = ctx.getServerHandler().player;
        if (player.openContainer != player.inventoryContainer) {
            cir.setReturnValue(null);
        }
    }

}
