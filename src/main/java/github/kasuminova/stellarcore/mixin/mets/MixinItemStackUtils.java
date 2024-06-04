package github.kasuminova.stellarcore.mixin.mets;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import ic2.api.item.IElectricItem;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import net.lrsoft.mets.util.ItemStackUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStackUtils.class)
public class MixinItemStackUtils {

    @Inject(method = "getCurrentTex", at = @At("HEAD"), cancellable = true, remap = false)
    private static void injectGetCurrentTex(final ItemStack stack, final int texLevel, final CallbackInfoReturnable<Integer> cir) {
        if (!StellarCoreConfig.FEATURES.ic2.electricItemNonDurability || !(stack.getItem() instanceof IElectricItem eItem)) {
            return;
        }

        NBTTagCompound tNBT = stack.getTagCompound();
        double charge = tNBT == null ? 0D : tNBT.getDouble("charge");
        double maxCharge = eItem.getMaxCharge(stack);

        int level;
        if (maxCharge > 0) {
            level = (int) Math.round(Util.limit((charge / maxCharge) * texLevel, 0, texLevel));
        } else {
            level = 0;
        }

        cir.setReturnValue(level);
    }

}
