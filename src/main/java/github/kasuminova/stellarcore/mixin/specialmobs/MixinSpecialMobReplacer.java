package github.kasuminova.stellarcore.mixin.specialmobs;

import com.llamalad7.mixinextras.sugar.Local;
import fathertoast.specialmobs.SpecialMobReplacer;
import fathertoast.specialmobs.bestiary.EnumMobFamily;
import github.kasuminova.stellarcore.common.util.StellarLog;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(SpecialMobReplacer.class)
public class MixinSpecialMobReplacer {

    @Inject(
            method = "replace",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V"
            ),
            remap = false
    )
    private void injectReplace(final EnumMobFamily mobFamily, final boolean isSpecial, final Entity entityToReplace, final World world, final BlockPos entityPos,
                               final CallbackInfo ci,
                               @Local(name = "var11") final Exception var11) {
        StellarLog.LOG.warn("Logging exception for SpecialMobs mod.", var11);
    }

}
