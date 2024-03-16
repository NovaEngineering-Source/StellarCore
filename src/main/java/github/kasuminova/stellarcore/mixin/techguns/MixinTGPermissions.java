package github.kasuminova.stellarcore.mixin.techguns;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import techguns.TGPermissions;

@Mixin(TGPermissions.class)
public class MixinTGPermissions {

    @Inject(method = "isPlayerOp", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onIsPlayerOp(final EntityPlayer player, final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.BUG_FIXES.techguns.tgPermissions) {
            return;
        }
        cir.setReturnValue(player.canUseCommand(4, ""));
    }

}
