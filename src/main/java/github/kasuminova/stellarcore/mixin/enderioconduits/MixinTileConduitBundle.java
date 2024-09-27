package github.kasuminova.stellarcore.mixin.enderioconduits;

import com.enderio.core.common.TileEntityBase;
import crazypants.enderio.base.conduit.IClientConduit;
import crazypants.enderio.base.conduit.IConduit;
import crazypants.enderio.base.conduit.IServerConduit;
import crazypants.enderio.conduits.conduit.TileConduitBundle;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import java.util.List;

@Mixin(TileConduitBundle.class)
public abstract class MixinTileConduitBundle extends TileEntityBase {

    @Shadow(remap = false) private boolean conduitsDirty;

    @Shadow(remap = false) protected abstract void doConduitsDirty();

    @Shadow(remap = false) protected abstract void updateEntityClient();

    @Shadow(remap = false) @Final @Nonnull private List<IServerConduit> serverConduits;

    @Shadow(remap = false) private List<IClientConduit> clientConduits;

    @Inject(method = "doUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectDoUpdate(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.enderIOConduits.tileConduitBundle) {
            return;
        }
        ci.cancel();

        // Server
        if (!this.world.isRemote) {
            for (final IConduit conduit : serverConduits) {
                conduit.updateEntity(this.world);
            }
            if (this.conduitsDirty) {
                this.doConduitsDirty();
            }
            return;
        }

        // Client
        if (clientConduits != null) {
            for (final IClientConduit conduit : clientConduits) {
                conduit.updateEntity(this.world);
            }
        }
        this.updateEntityClient();
    }

}
