package github.kasuminova.stellarcore.mixin.ic2;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import ic2.core.item.DamageHandler;
import ic2.core.item.ElectricItemManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(ElectricItemManager.class)
public class MixinElectricItemManager {

    @Redirect(
            method = "charge",
            at = @At(
                    value = "INVOKE",
                    target = "Lic2/core/item/DamageHandler;setDamage(Lnet/minecraft/item/ItemStack;IZ)V",
                    remap = false
            ),
            remap = false
    )
    private void redirectChargeSetDamage(final ItemStack stack, final int damage, final boolean displayOnly) {
        if (!StellarCoreConfig.FEATURES.ic2.electricItemNonDurability) {
            DamageHandler.setDamage(stack, damage, displayOnly);
            return;
        }
        if (DamageHandler.getDamage(stack) != 0) {
            // Always no damage
            DamageHandler.setDamage(stack, 0, displayOnly);
        }
    }

    @Redirect(
            method = "discharge",
            at = @At(
                    value = "INVOKE",
                    target = "Lic2/core/item/DamageHandler;setDamage(Lnet/minecraft/item/ItemStack;IZ)V",
                    remap = false
            ),
            remap = false
    )
    private void redirectDischargeSetDamage(final ItemStack stack, final int damage, final boolean displayOnly) {
        if (!StellarCoreConfig.FEATURES.ic2.electricItemNonDurability) {
            DamageHandler.setDamage(stack, damage, displayOnly);
            return;
        }
        if (DamageHandler.getDamage(stack) != 0) {
            // Always no damage
            DamageHandler.setDamage(stack, 0, displayOnly);
        }
    }

}
