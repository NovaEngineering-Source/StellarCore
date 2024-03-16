package github.kasuminova.stellarcore.mixin.mets;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.lrsoft.mets.item.ElectricFirstAidLifeSupport;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(ElectricFirstAidLifeSupport.class)
public class MixinFirstAidLifeSupport {

    /**
     * 电力生命维护仪。
     * （急救失败！）
     */
    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true)
    private void injectOnWorkTick(final ItemStack stack, final World worldIn, final Entity entity, final int itemSlot, final boolean isSelected, final CallbackInfo ci) {
        if (!StellarCoreConfig.BUG_FIXES.moreElectricTools.fixLifeSupports) {
            return;
        }
        if (entity == null || !entity.isEntityAlive()) {
            ci.cancel();
        }
    }

}
