package github.kasuminova.stellarcore.mixin.enderioconduits_energy;

import crazypants.enderio.base.power.IPowerInterface;
import crazypants.enderio.base.power.PowerHandlerUtil;
import crazypants.enderio.conduits.conduit.power.IPowerConduit;
import crazypants.enderio.conduits.conduit.power.PowerConduitNetwork;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;

@Mixin(value = PowerConduitNetwork.ReceptorEntry.class, remap = false)
public class MixinReceptorEntry {

    @Shadow
    @Final
    @Nonnull
    IPowerConduit emmiter;

    @Shadow
    @Final
    @Nonnull
    BlockPos pos;

    @Shadow
    @Final
    @Nonnull
    EnumFacing direction;

    @Inject(method = "getPowerInterface", at = @At("HEAD"), cancellable = true)
    private void getPowerInterface(final CallbackInfoReturnable<IPowerInterface> cir) {
        if (!StellarCoreConfig.PERFORMANCE.enderIOConduits.networkPowerManager) {
            return;
        }
        World world = emmiter.getBundle().getBundleworld();
        cir.setReturnValue(
                world.isBlockLoaded(pos)
                        ? PowerHandlerUtil.getPowerInterface(world.getTileEntity(pos), direction.getOpposite())
                        : null
        );
    }

}
