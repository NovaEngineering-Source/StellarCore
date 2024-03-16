package github.kasuminova.stellarcore.mixin.mets;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.lrsoft.mets.item.bauble.ElectricLifeSupportRing;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(ElectricLifeSupportRing.class)
public class MixinLifeSupportRing {

    /**
     * 终极维生指环
     * （神已经跌落神坛了。）
     */
    @Inject(method = "onWornTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectOnWorkTick(final ItemStack itemstack, final EntityLivingBase entity, final CallbackInfo ci) {
        if (!StellarCoreConfig.BUG_FIXES.moreElectricTools.fixLifeSupports) {
            return;
        }
        if (entity == null || !entity.isEntityAlive()) {
            ci.cancel();
        }
    }

}
