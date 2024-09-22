package github.kasuminova.stellarcore.mixin.enderioconduits_energy;

import crazypants.enderio.base.power.IPowerInterface;
import crazypants.enderio.conduits.conduit.power.IPowerConduit;
import crazypants.enderio.conduits.conduit.power.PowerConduitNetwork;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = PowerConduitNetwork.ReceptorEntry.class, remap = false)
public interface AccessorReceptorEntry {

    @Invoker
    IPowerInterface invokeGetPowerInterface();

    @Accessor
    IPowerConduit getEmmiter();

    @Accessor
    BlockPos getPos();

    @Accessor
    EnumFacing getDirection();

}
