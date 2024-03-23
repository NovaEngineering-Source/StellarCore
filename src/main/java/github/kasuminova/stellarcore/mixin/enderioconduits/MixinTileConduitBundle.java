package github.kasuminova.stellarcore.mixin.enderioconduits;

import com.enderio.core.common.TileEntityBase;
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
public abstract class MixinTileConduitBundle extends TileEntityBase {

    @Shadow(remap = false) public abstract Collection<? extends IConduit> getConduits();

    @Shadow(remap = false) private boolean conduitsDirty;

    @Shadow(remap = false) protected abstract void doConduitsDirty();

    @Shadow(remap = false) protected abstract void updateEntityClient();

    @Inject(method = "doUpdate", at = @At("HEAD"), cancellable = true, remap = false)
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
