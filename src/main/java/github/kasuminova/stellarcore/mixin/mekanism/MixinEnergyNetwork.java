package github.kasuminova.stellarcore.mixin.mekanism;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import mekanism.api.Coord4D;
import mekanism.api.energy.EnergyStack;
import mekanism.api.transmitters.DynamicNetwork;
import mekanism.common.base.EnergyAcceptorWrapper;
import mekanism.common.base.target.EnergyAcceptorTarget;
import mekanism.common.transmitters.grid.EnergyNetwork;
import mekanism.common.util.EmitUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;
import java.util.Set;

@Mixin(EnergyNetwork.class)
public abstract class MixinEnergyNetwork extends DynamicNetwork<EnergyAcceptorWrapper, EnergyNetwork, EnergyStack> {

    @Unique
    private final Set<EnergyAcceptorTarget> stellar_core$targets = new ObjectOpenHashSet<>();

    @Inject(method = "tickEmit", at = @At("HEAD"), cancellable = true, remap = false)
    public void injectTickEmit(final double energyToSend, final CallbackInfoReturnable<Double> cir) {
        if (!StellarCoreConfig.PERFORMANCE.mekanism.energyNetwork) {
            return;
        }
        stellar_core$targets.clear();
        int totalHandlers = 0;

        for (Coord4D coord : this.possibleAcceptors) {
            EnumSet<EnumFacing> sides = this.acceptorDirections.get(coord);
            if (sides == null || sides.isEmpty()) {
                continue;
            }
            TileEntity tile = coord.getTileEntity(this.getWorld());
            if (tile == null) {
                continue;
            }
            EnergyAcceptorTarget target = new EnergyAcceptorTarget();

            for (EnumFacing side : sides) {
                EnergyAcceptorWrapper acceptor = EnergyAcceptorWrapper.get(tile, side);
                if (acceptor != null && acceptor.canReceiveEnergy(side) && acceptor.needsEnergy(side)) {
                    target.addHandler(side, acceptor);
                }
            }

            int curHandlers = target.getHandlers().size();
            if (curHandlers > 0) {
                stellar_core$targets.add(target);
                totalHandlers += curHandlers;
            }
        }

        cir.setReturnValue(EmitUtils.sendToAcceptors(stellar_core$targets, totalHandlers, energyToSend));
    }

}
