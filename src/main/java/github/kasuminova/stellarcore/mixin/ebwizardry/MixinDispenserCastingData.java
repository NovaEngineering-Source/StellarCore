package github.kasuminova.stellarcore.mixin.ebwizardry;

import electroblob.wizardry.data.DispenserCastingData;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DispenserCastingData.class, remap = false)
public abstract class MixinDispenserCastingData {

    @Inject(method = "onWorldTickEvent", at = @At("HEAD"), cancellable = true)
    private static void injectOnWorldTickEvent(final TickEvent.WorldTickEvent event, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.ebWizardry.dispenserCastingData) {
            return;
        }
        ci.cancel();
    }

}
