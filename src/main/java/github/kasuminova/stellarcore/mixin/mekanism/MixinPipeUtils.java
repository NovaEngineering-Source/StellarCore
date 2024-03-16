package github.kasuminova.stellarcore.mixin.mekanism;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import mekanism.common.base.target.FluidHandlerTarget;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.EmitUtils;
import mekanism.common.util.PipeUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Set;

@Mixin(PipeUtils.class)
public class MixinPipeUtils {

    /**
     * @author Kasumi_Nova
     * @reason 没有 HashSet 就没有伤害。
     */
    @Inject(method = "emit", at = @At("HEAD"), cancellable = true, remap = false)
    private static void emit(final Set<EnumFacing> sides, final FluidStack stack, final TileEntity from, final CallbackInfoReturnable<Integer> cir) {
        if (!StellarCoreConfig.PERFORMANCE.mekanism.pipeUtils) {
            return;
        }
        if (stack == null || stack.amount == 0) {
            cir.setReturnValue(0);
            return;
        }

        FluidHandlerTarget target = new FluidHandlerTarget(stack);
        EmitUtils.forEachSide(from.getWorld(), from.getPos(), sides, (acceptor, side) -> {
            EnumFacing accessSide = side.getOpposite();
            CapabilityUtils.runIfCap(acceptor, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, accessSide, (handler) -> {
                if (PipeUtils.canFill(handler, stack)) {
                    target.addHandler(accessSide, handler);
                }
            });
        });

        int curHandlers = target.getHandlers().size();
        if (curHandlers == 0) {
            cir.setReturnValue(0);
            return;
        }

        cir.setReturnValue(EmitUtils.sendToAcceptors(Collections.singleton(target), curHandlers, stack.amount, stack));
    }

}
