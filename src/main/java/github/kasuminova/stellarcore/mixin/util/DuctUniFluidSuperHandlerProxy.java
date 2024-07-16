package github.kasuminova.stellarcore.mixin.util;

import cofh.thermaldynamics.duct.fluid.DuctUnitFluidSuper;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

public class DuctUniFluidSuperHandlerProxy implements IFluidHandler {

    private final DuctUnitFluidSuper duct;
    private final IFluidHandler proxied;
    private final EnumFacing from;

    public DuctUniFluidSuperHandlerProxy(final DuctUnitFluidSuper duct, final IFluidHandler proxied, final EnumFacing from) {
        this.duct = duct;
        this.proxied = proxied;
        this.from = from;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return proxied.getTankProperties();
    }

    @Override
    public int fill(final FluidStack resource, final boolean doFill) {
        return proxied.fill(resource, doFill);
    }

    @Nullable
    @Override
    public FluidStack drain(final FluidStack resource, final boolean doDrain) {
        //noinspection DataFlowIssue
        return duct.isOpen(from) ? duct.getGrid().myTank.drain(resource, doDrain) : null;
    }

    @Nullable
    @Override
    public FluidStack drain(final int maxDrain, final boolean doDrain) {
        return proxied.drain(maxDrain, doDrain);
    }

}
