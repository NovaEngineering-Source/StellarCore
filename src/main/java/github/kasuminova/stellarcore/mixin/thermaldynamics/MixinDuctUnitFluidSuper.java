package github.kasuminova.stellarcore.mixin.thermaldynamics;

import cofh.thermaldynamics.duct.fluid.DuctUnitFluidSuper;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.DuctUniFluidSuperHandlerProxy;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DuctUnitFluidSuper.class)
public class MixinDuctUnitFluidSuper {

    @ModifyReturnValue(method = "getFluidCapability", at = @At("RETURN"), remap = false)
    private IFluidHandler modifyFluidCapability(final IFluidHandler handler, final EnumFacing from) {
        if (!StellarCoreConfig.BUG_FIXES.thermalDynamics.fixFluidDuplicate) {
            return handler;
        }
        if (handler == EmptyFluidHandler.INSTANCE) {
            return handler;
        }
        return new DuctUniFluidSuperHandlerProxy((DuctUnitFluidSuper) (Object) this, handler, from);
    }

}
