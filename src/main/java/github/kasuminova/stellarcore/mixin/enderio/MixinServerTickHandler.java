package github.kasuminova.stellarcore.mixin.enderio;

import crazypants.enderio.base.handler.ServerTickHandler;
import github.kasuminova.stellarcore.mixin.util.IStellarServerTickListener;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.profiler.Profiler;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ServerTickHandler.class, remap = false)
public class MixinServerTickHandler {

    @Unique
    private static final List<IStellarServerTickListener> FINAL_TICK_LISTENERS = new ObjectArrayList<>();

    @Inject(
            method = "lambda$onWorldTick$3",
            at = @At(
                    value = "INVOKE",
                    target = "Lcrazypants/enderio/base/handler/ServerTickHandler$IServerTickListener;tickEnd(Lnet/minecraft/profiler/Profiler;)V",
                    shift = At.Shift.AFTER
            )
    )
    private static void injectOnWorldTickTickListenerEnd(final Profiler profiler, final TickEvent.WorldTickEvent event, final ServerTickHandler.IServerTickListener listener, final String name, final CallbackInfo ci) {
        if (listener instanceof IStellarServerTickListener stellarListener) {
            FINAL_TICK_LISTENERS.add(stellarListener);
        }
    }

    @Inject(method = "onWorldTick", at = @At("RETURN"))
    private static void injectOnWorldTickEnd(final TickEvent.WorldTickEvent event, final CallbackInfo ci) {
        FINAL_TICK_LISTENERS.forEach(IStellarServerTickListener::tickFinal);
        FINAL_TICK_LISTENERS.clear();
    }

}
