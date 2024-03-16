package github.kasuminova.stellarcore.mixin.mets;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.lrsoft.mets.item.ElectricForceFieldGenerator;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(ElectricForceFieldGenerator.class)
public class MixinForceFieldGenerator {

    /**
     * 电力伤害吸收仪
     * （虽然死灵护盾挺合理的，但是还是有点不太对劲...）
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
