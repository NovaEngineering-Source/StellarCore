package github.kasuminova.stellarcore.mixin.mets;

import github.kasuminova.stellarcore.mixin.util.EntityLivingBaseValues;
import net.lrsoft.mets.entity.EntityTachyonBullet;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(EntityTachyonBullet.class)
public class MixinEntityTachyonBullet {

    /**
     * 杀杀杀杀杀
     */
    @Redirect(
            method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;setHealth(F)V"
            )
    )
    private void redirectSetHealth(final EntityLivingBase instance, final float health) {
        DataParameter<Float> healthType = EntityLivingBaseValues.HEALTH;
        if (healthType != null) {
            instance.getDataManager().set(healthType, MathHelper.clamp(health, 0, instance.getMaxHealth()));
            return;
        }
        instance.setHealth(health);
    }

}
