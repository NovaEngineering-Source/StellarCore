package github.kasuminova.stellarcore.mixin.extrabotany;

import com.google.common.util.concurrent.ListenableFuture;
import com.meteor.extrabotany.client.ClientProxy;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;

@Mixin(ClientProxy.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinClientProxy {

    /**
     * addScheduledTask 不代表异步。
     */
    @Redirect(
            method = "preInit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;addScheduledTask(Ljava/lang/Runnable;)Lcom/google/common/util/concurrent/ListenableFuture;",
                    remap = true
            ),
            remap = false)
    private ListenableFuture<Object> redirectAddScheduledTask(final Minecraft instance, final Runnable runnableToSchedule) {
        CompletableFuture.runAsync(runnableToSchedule);
        // 返回 null，完全用不到。
        return null;
    }
}
