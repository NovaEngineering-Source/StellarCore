package github.kasuminova.stellarcore.mixin.forge.memory;

import net.minecraftforge.fml.client.SplashProgress;
import org.lwjgl.BufferUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;

@Mixin(value = SplashProgress.class, remap = false)
public abstract class MixinSplashUploadBufferLifecycle {
    @Shadow
    @Final
    @Mutable
    private static IntBuffer buf;

    @Shadow
    private static Thread thread;

    @Inject(method = "start", at = @At("HEAD"), require = 1)
    private static void stellar_core$restoreUploadBuffer(final CallbackInfo ci) {
        if (buf == null) buf = BufferUtils.createIntBuffer(4194304);
    }

    @Inject(method = "finish", at = @At("RETURN"), require = 1)
    private static void stellar_core$releaseFinishedUploadBuffer(final CallbackInfo ci) {
        // finish also catches interrupted/failed joins. Never detach a live splash thread's storage.
        if (thread == null || !thread.isAlive()) buf = null;
    }
}
