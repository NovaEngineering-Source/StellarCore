package github.kasuminova.stellarcore.mixin.sync_techguns;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import me.ichun.mods.sync.common.tileentity.TileEntityDualVertical;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import techguns.api.capabilities.ITGExtendedPlayer;
import techguns.capabilities.TGExtendedPlayerCapProvider;


@Mixin(TileEntityDualVertical.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinTileEntityDualVerticalTechguns {

    @Inject(method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayerMP;writeToNBT(Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/nbt/NBTTagCompound;"
            )
    )
    private void injectUpdateClearInventory(final CallbackInfo ci, @Local(name = "dummy") EntityPlayerMP dummy) {
        if (!StellarCoreConfig.BUG_FIXES.sync.techgunsDuplicationFixes) {
            return;
        }
        ITGExtendedPlayer tgCap = dummy.getCapability(TGExtendedPlayerCapProvider.TG_EXTENDED_PLAYER, null);
        if (tgCap != null) {
            tgCap.getTGInventory().clear();
        }
    }

}
