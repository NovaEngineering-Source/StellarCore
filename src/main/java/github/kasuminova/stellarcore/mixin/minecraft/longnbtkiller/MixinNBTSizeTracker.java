package github.kasuminova.stellarcore.mixin.minecraft.longnbtkiller;

import github.kasuminova.stellarcore.StellarCore;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import io.netty.util.internal.ThrowableUtil;
import net.minecraft.nbt.NBTSizeTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NBTSizeTracker.class)
public class MixinNBTSizeTracker {

    @Final
    @Shadow
    @Mutable
    private long max;

    @Shadow
    private long read;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final long max, final CallbackInfo ci) {
        this.max = Integer.MAX_VALUE;
    }

    @Inject(method = "read", at = @At("HEAD"), cancellable = true)
    private void injectRead(final long bits, final CallbackInfo ci) {
        ci.cancel();

        this.read += bits / 8L;
        if (this.read < StellarCoreConfig.BUG_FIXES.vanilla.maxNBTSize || !StellarCoreConfig.BUG_FIXES.vanilla.displayLargeNBTWarning) {
            return;
        }

        StellarLog.LOG.warn("Detected large nbt size: {}", this.read);
        try {
            throw new RuntimeException("");
        } catch (RuntimeException e) {
            StellarLog.LOG.warn(ThrowableUtil.stackTraceToString(e));
        }
    }

}
