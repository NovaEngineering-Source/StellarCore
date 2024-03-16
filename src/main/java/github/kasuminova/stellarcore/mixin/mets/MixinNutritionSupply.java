package github.kasuminova.stellarcore.mixin.mets;

import net.lrsoft.mets.item.ElectricNutritionSupply;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(ElectricNutritionSupply.class)
public class MixinNutritionSupply {

    /**
     * 电力营养供应器
     * （死因是撑死。）
     */
    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true)
    private void injectOnWorkTick(final ItemStack stack, final World worldIn, final Entity entity, final int itemSlot, final boolean isSelected, final CallbackInfo ci) {
        if (entity == null || !entity.isEntityAlive()) {
            ci.cancel();
        }
    }

}
