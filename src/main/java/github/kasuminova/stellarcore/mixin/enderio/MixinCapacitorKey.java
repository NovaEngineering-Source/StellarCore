package github.kasuminova.stellarcore.mixin.enderio;

import crazypants.enderio.base.capacitor.CapacitorKey;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(value = CapacitorKey.class, remap = false)
public abstract class MixinCapacitorKey {

    @Unique
    private String stellar_core$legacyName;

    @Inject(method = "getLegacyName", at = @At("HEAD"), cancellable = true)
    private void stellar_core$cachedLegacyName(final CallbackInfoReturnable<String> cir) {
        if (!StellarCoreConfig.PERFORMANCE.enderIO.capacitorKeyLegacyNameCache) {
            return;
        }
        String cached = stellar_core$legacyName;
        if (cached == null) {
            cached = ((CapacitorKey) (Object) this).name().toLowerCase(Locale.ENGLISH);
            stellar_core$legacyName = cached;
        }
        cir.setReturnValue(cached);
    }

}
