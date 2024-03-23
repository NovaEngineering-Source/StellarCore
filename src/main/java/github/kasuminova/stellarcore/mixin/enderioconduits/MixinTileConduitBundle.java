package github.kasuminova.stellarcore.mixin.enderioconduits;

import crazypants.enderio.base.TileEntityEio;
import crazypants.enderio.base.conduit.IConduit;
import crazypants.enderio.conduits.conduit.TileConduitBundle;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(TileConduitBundle.class)
public abstract class MixinTileConduitBundle extends TileEntityEio {

    @Shadow public abstract Collection<? extends IConduit> getConduits();

    @Shadow private boolean conduitsDirty;

    @Shadow protected abstract void doConduitsDirty();

    @Shadow protected abstract void updateEntityClient();

    @Inject(method = "doUpdate", at = @At("HEAD"), cancellable = true)
    private void injectDoUpdate(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.enderIOConduits.tileConduitBundle) {
            return;
        }

        for (final IConduit conduit : this.getConduits()) {
            conduit.updateEntity(this.world);
        }

        if (!this.world.isRemote && this.conduitsDirty) {
            this.doConduitsDirty();
        }

        if (this.world.isRemote) {
            this.updateEntityClient();
        }

        ci.cancel();
    }

}
