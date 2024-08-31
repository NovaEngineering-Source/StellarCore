package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultResourcePack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Final
    @Shadow
    public DefaultResourcePack defaultResourcePack;

    @Inject(method = "refreshResources", at = @At("HEAD"))
    private void injectRefreshResourcesBefore(final CallbackInfo ci) {
        if (defaultResourcePack instanceof StellarCoreResourcePack reloadListener) {
            reloadListener.stellar_core$onReload();
        }
    }

}
