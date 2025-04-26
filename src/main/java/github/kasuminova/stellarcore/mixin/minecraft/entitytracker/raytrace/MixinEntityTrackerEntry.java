package github.kasuminova.stellarcore.mixin.minecraft.entitytracker.raytrace;

import com.llamalad7.mixinextras.sugar.Local;
import dev.tr7zw.entityculling.versionless.access.Cullable;
import net.minecraft.entity.Entity;
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
    private Entity trackedEntity;
    @Inject(method = "isVisibleTo", at = @At("RETURN"), cancellable = true)
    private void onIsVisibleTo(EntityPlayerMP playerMP, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) int range) {
        if (cir.getReturnValue()) {
            if (((Cullable) trackedEntity).isCulled()) {
                cir.setReturnValue(false);
            }
        }
    }
}
