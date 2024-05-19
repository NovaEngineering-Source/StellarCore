package github.kasuminova.stellarcore.mixin.ftbquests;

import com.feed_the_beast.ftbquests.util.FTBQuestsInventoryListener;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.integration.ftbquests.FTBQInvListener;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(FTBQuestsInventoryListener.class)
public class MixinFTBQuestsInventoryListener {

    @Redirect(
            method = "sendSlotContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/feed_the_beast/ftbquests/util/FTBQuestsInventoryListener;detect(Lnet/minecraft/entity/player/EntityPlayerMP;Lnet/minecraft/item/ItemStack;I)V",
                    remap = false
            ),
            remap = true
    )
    private void redirectSendSlotContentsDetect(final EntityPlayerMP player, final ItemStack item, final int sourceTask) {
        if (!StellarCoreConfig.PERFORMANCE.ftbQuests.questInventoryListener) {
            FTBQuestsInventoryListener.detect(player, item, sourceTask);
            return;
        }
        FTBQInvListener.INSTANCE.addRequiredToDetect(player);
    }

    @Redirect(
            method = "sendAllContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/feed_the_beast/ftbquests/util/FTBQuestsInventoryListener;detect(Lnet/minecraft/entity/player/EntityPlayerMP;Lnet/minecraft/item/ItemStack;I)V",
                    remap = false
            ),
            remap = true
    )
    private void redirectSendAllContentsDetect(final EntityPlayerMP player, final ItemStack item, final int sourceTask) {
        if (!StellarCoreConfig.PERFORMANCE.ftbQuests.questInventoryListener) {
            FTBQuestsInventoryListener.detect(player, item, sourceTask);
            return;
        }
        FTBQInvListener.INSTANCE.addRequiredToDetect(player);
    }

}
