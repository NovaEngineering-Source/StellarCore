package github.kasuminova.stellarcore.mixin.appeng;

import appeng.fluids.client.render.FluidStackSizeRenderer;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = FluidStackSizeRenderer.class, remap = false)
public class MixinFluidStackSizeRenderer {

    @ModifyConstant(method = "renderStackSize", constant = @Constant(floatValue = 0.5F))
    public float onRenderStackSize(final float ci) {
        return StellarCoreConfig.FEATURES.fontScale.ae2;
    }

    @ModifyConstant(method = "renderStackSize", constant = {@Constant(intValue = 0), @Constant(intValue = -1)})
    public int onRenderStackSize(final int ci) {
        if (ci == -1) {
            return 0;
        }
        return ci;
    }

}
