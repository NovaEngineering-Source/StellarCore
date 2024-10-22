package github.kasuminova.stellarcore.mixin.minecraft.nethandlerplayserver;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayServer {

    @Shadow
    public EntityPlayerMP player;

    @Inject(
            method = {"processTryUseItemOnBlock", "processTryUseItem"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;getWorld(I)Lnet/minecraft/world/WorldServer;"
            ),
            cancellable = true
    )
    private void injectProcessTryUseItemOnBlock(final CallbackInfo ci) {
        if (this.player.openContainer != this.player.inventoryContainer) {
            ci.cancel();
        }
    }

}
