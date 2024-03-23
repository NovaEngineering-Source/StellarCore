package github.kasuminova.stellarcore.mixin.sync;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import me.ichun.mods.sync.common.tileentity.TileEntityDualVertical;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityDualVertical.class)
public class MixinTileEntityDualVertical {

    @Inject(method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayerMP;setHealth(F)V"
            )
    )
    private void injectUpdateClearInventory(final CallbackInfo ci, @Local(name = "player") EntityPlayerMP player) {
        if (!StellarCoreConfig.BUG_FIXES.sync.ridingFixes) {
            return;
        }
        if (player.isRiding()) {
            player.dismountRidingEntity();
        }
    }

}
