package github.kasuminova.stellarcore.mixin.eioconduit;

import crazypants.enderio.base.conduit.IConduitBundle;
import crazypants.enderio.conduits.conduit.AbstractConduit;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;

@Mixin(AbstractConduit.class)
public abstract class MixinAbstractConduit {

    @Shadow(remap = false) protected abstract void updateNetwork(final World world);

    @Shadow(remap = false) protected abstract void updateConnections();

    @Shadow(remap = false) protected boolean readFromNbt;

    @Shadow(remap = false)private boolean clientStateDirty;

    @Shadow(remap = false) @Nonnull public abstract IConduitBundle getBundle();

    /**
     * @author Kasumi_Nova
     * @reason 移除 Profiler 部分。
     */
    @Overwrite(remap = false)
    public void updateEntity(@Nonnull World world) {
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
