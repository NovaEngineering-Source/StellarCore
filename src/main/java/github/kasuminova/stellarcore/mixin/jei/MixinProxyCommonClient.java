package github.kasuminova.stellarcore.mixin.jei;

import github.kasuminova.stellarcore.client.texture.StitcherCache;
import mezz.jei.gui.textures.JeiTextureMap;
import mezz.jei.startup.ProxyCommonClient;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(ProxyCommonClient.class)
public class MixinProxyCommonClient {

    @Final
    @Shadow(remap = false)
    private JeiTextureMap textureMap;

    @Inject(method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureManager;loadTickableTexture(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/ITickableTextureObject;)Z",
                    remap = true
            ),
            remap = false
    )
    private void injectInitBefore(final FMLInitializationEvent event, final CallbackInfo ci) {
        StitcherCache.create("jei", textureMap);
        StitcherCache.setActiveMap(textureMap);
    }

    @Inject(method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureManager;loadTickableTexture(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/ITickableTextureObject;)Z",
                    shift = At.Shift.AFTER,
                    remap = true
            ),
            remap = false
    )
    private void injectInitAfter(final FMLInitializationEvent event, final CallbackInfo ci) {
        StitcherCache.setActiveMap(null);
    }

}
