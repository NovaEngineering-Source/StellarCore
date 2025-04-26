package github.kasuminova.stellarcore.mixin.minecraft.entitytracker.y_range;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityTrackerEntry.class)
public class MixinEntityTrackerEntry {
    @Shadow
    private long encodedPosY;
    @Unique
    private static final int ENTITY_Y_TRACKING_RANGE = Integer.getInteger("stellarcore.entitytracker.y_range", -1);

    @Inject(method = "isVisibleTo", at = @At("RETURN"), cancellable = true)
    private void onIsVisibleTo(EntityPlayerMP playerMP, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) int range) {
        if (cir.getReturnValue()) {
            double yDistance = Math.abs(playerMP.posY - (double) this.encodedPosY / 4096.0D);
            if ((ENTITY_Y_TRACKING_RANGE > 0 && yDistance > ENTITY_Y_TRACKING_RANGE) || yDistance > range) {
                cir.setReturnValue(false);
            }
        }
    }
}
