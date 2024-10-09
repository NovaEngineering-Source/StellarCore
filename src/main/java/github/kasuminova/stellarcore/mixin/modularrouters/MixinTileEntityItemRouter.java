package github.kasuminova.stellarcore.mixin.modularrouters;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import me.desht.modularrouters.block.tile.TileEntityItemRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityItemRouter.class, remap = false)
public class MixinTileEntityItemRouter {

    @Shadow
    private boolean ecoMode;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        if (!StellarCoreConfig.FEATURES.modularRouters.routerECOModeByDefault) {
            return;
        }
        this.ecoMode = true;
    }

}
