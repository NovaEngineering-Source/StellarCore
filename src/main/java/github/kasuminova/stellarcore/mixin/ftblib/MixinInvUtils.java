package github.kasuminova.stellarcore.mixin.ftblib;

import com.feed_the_beast.ftblib.lib.util.InvUtils;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.integration.ftblib.FTBLibInvUtilsQueue;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InvUtils.class)
public class MixinInvUtils {

    @Inject(
            method = "forceUpdate(Lnet/minecraft/entity/player/EntityPlayer;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void redirectPlayerForceUpdate(final EntityPlayer player, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.ftbLib.invUtilForceUpdate) {
            return;
        }
        FTBLibInvUtilsQueue.INSTANCE.enqueuePlayer(player);
        ci.cancel();
    }

}
