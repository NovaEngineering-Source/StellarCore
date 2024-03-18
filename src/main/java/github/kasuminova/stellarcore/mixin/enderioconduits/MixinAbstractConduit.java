package github.kasuminova.stellarcore.mixin.enderioconduits;

import crazypants.enderio.base.conduit.IConduitBundle;
import crazypants.enderio.conduits.conduit.AbstractConduit;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;

@Mixin(AbstractConduit.class)
public abstract class MixinAbstractConduit {

    @Shadow(remap = false) protected abstract void updateNetwork(final World world);

    @Shadow(remap = false) protected abstract void updateConnections();

    @Shadow(remap = false) protected boolean readFromNbt;

    @Shadow(remap = false) private boolean clientStateDirty;

    @Shadow(remap = false) @Nonnull public abstract IConduitBundle getBundle();

    /**
     * @author Kasumi_Nova
     * @reason 移除 Profiler 部分。
     */
    @Inject(method = "updateEntity", at = @At("HEAD"), cancellable = true, remap = false)
    public void updateEntity(final World world, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.enderIOConduits.abstractConduit) {
            return;
        }
        ci.cancel();

        // Improvements
        if (world.isRemote) {
            return;
        }
        updateNetwork(world);
        updateConnections();
        readFromNbt = false; // the two update*()s react to this on their first run
        if (clientStateDirty) {
            getBundle().dirty();
            clientStateDirty = false;
        }
    }

}
