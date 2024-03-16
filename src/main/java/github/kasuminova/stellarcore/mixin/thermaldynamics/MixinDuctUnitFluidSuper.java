package github.kasuminova.stellarcore.mixin.thermaldynamics;

import cofh.thermaldynamics.duct.fluid.DuctUnitFluidSuper;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;

@Mixin(DuctUnitFluidSuper.class)
public class MixinDuctUnitFluidSuper {

    @ModifyReturnValue(method = "getFluidCapability", at = @At("RETURN"), remap = false)
    private IFluidHandler modifyFluidCapability(final IFluidHandler handler, final EnumFacing from) {
        if (handler == EmptyFluidHandler.INSTANCE) {
            return handler;
        }

        DuctUnitFluidSuper duct = (DuctUnitFluidSuper) (Object) this;
        //noinspection AnonymousInnerClassMayBeStatic
        return new IFluidHandler() {
            @Override
            public IFluidTankProperties[] getTankProperties() {
                return handler.getTankProperties();
            }

            @Override
            public int fill(final FluidStack resource, final boolean doFill) {
                return handler.fill(resource, doFill);
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
                return handler.drain(maxDrain, doDrain);
            }
        };
    }

}
