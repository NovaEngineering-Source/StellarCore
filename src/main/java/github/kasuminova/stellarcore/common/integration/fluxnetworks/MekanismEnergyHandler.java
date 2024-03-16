package github.kasuminova.stellarcore.common.integration.fluxnetworks;

import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.api.energy.IStrictEnergyOutputter;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import sonar.fluxnetworks.api.energy.ITileEnergyHandler;

import javax.annotation.Nonnull;

public class MekanismEnergyHandler implements ITileEnergyHandler {

    public static final MekanismEnergyHandler INSTANCE = new MekanismEnergyHandler();

    @Override
    public boolean hasCapability(@Nonnull final TileEntity te, final EnumFacing facing) {
        return canAddEnergy(te, facing) || canRemoveEnergy(te, facing);
    }

    @Override
    public boolean canAddEnergy(@Nonnull final TileEntity te, final EnumFacing facing) {
        return te instanceof IStrictEnergyAcceptor acceptor && acceptor.canReceiveEnergy(facing);
    }

    @Override
    public boolean canRemoveEnergy(@Nonnull final TileEntity te, final EnumFacing facing) {
        return te instanceof IStrictEnergyOutputter outputter && outputter.canOutputEnergy(facing);
    }

    @Override
    public long addEnergy(final long amount, @Nonnull final TileEntity te, final EnumFacing facing, final boolean simulate) {
        if (te instanceof IStrictEnergyAcceptor acceptor) {
            return (long) Math.ceil(acceptor.acceptEnergy(facing, ((double) amount) * 2.5D, simulate) / 2.5D);
        }
        return 0;
    }

    @Override
    public long removeEnergy(final long amount, @Nonnull final TileEntity te, final EnumFacing facing) {
        if (te instanceof IStrictEnergyOutputter outputter) {
            return (long) Math.ceil(outputter.pullEnergy(facing, ((double) amount) * 2.5D, false) / 2.5D);
        }
        return 0;
    }
}
