package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePackReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultResourcePack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Final
    @Shadow
    public DefaultResourcePack defaultResourcePack;

    @Inject(method = "refreshResources", at = @At("HEAD"))
    private void injectRefreshResources(final CallbackInfo ci) {
        if (defaultResourcePack instanceof StellarCoreResourcePackReloadListener reloadListener) {
            reloadListener.stellar_core$onReload();
        }
    }

}
