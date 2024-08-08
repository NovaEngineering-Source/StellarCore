package github.kasuminova.stellarcore.mixin.minecraft.world_load;

import github.kasuminova.stellarcore.client.handler.ClientEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void injectLoadWorld(final WorldClient worldClientIn, final String loadingMessage, final CallbackInfo ci) {
        ClientEventHandler.INSTANCE.onClientWorldLoad(worldClientIn);
    }

}
