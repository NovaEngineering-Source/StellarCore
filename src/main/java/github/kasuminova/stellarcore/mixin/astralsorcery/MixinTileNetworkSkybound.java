package github.kasuminova.stellarcore.mixin.astralsorcery;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import hellfirepvp.astralsorcery.common.tile.base.TileNetwork;
import hellfirepvp.astralsorcery.common.tile.base.TileNetworkSkybound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileNetworkSkybound.class, remap = false)
public class MixinTileNetworkSkybound extends TileNetwork {

    @Shadow
    protected boolean doesSeeSky;

    @Inject(method = "updateSkyState", at = @At("HEAD"), cancellable = true)
    protected void updateSkyState(final boolean seesSky, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.astralSorcery.tileNetworkSkybound) {
            return;
        }

        if (this.doesSeeSky != seesSky) {
            this.doesSeeSky = seesSky;
            markForUpdate();
        }

        ci.cancel();
    }

}
