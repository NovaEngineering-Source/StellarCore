package github.kasuminova.stellarcore.common.integration.fluxnetworks;

import mekanism.common.base.EnergyAcceptorWrapper;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.CapabilityEnergy;
import sonar.fluxnetworks.common.tileentity.TileFluxPlug;

public class FluxPlugAcceptor extends EnergyAcceptorWrapper.ForgeAcceptor {

    private final TileFluxPlug plug;

    public FluxPlugAcceptor(final TileFluxPlug plug, final EnumFacing side) {
        super(plug.getCapability(CapabilityEnergy.ENERGY, side));
        this.plug = plug;
    }

    @Override
    public double acceptEnergy(final EnumFacing facing, final double amount, final boolean simulate) {
        double maxCanReceive = Math.min(plug.getMaxTransferLimit() - plug.getTransferBuffer(), amount / 2.5D);
        if (maxCanReceive >= Long.MAX_VALUE) {
            maxCanReceive = Long.MAX_VALUE;
        }
        return plug.getTransferHandler().receiveFromSupplier((long) maxCanReceive, facing, simulate) * 2.5D;
    }
}
