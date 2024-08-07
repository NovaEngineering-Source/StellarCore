package github.kasuminova.stellarcore.mixin.appeng;

import appeng.client.render.StackSizeRenderer;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 允许自行调节小字体的缩放。
 */
@Mixin(value = StackSizeRenderer.class, remap = false)
@SuppressWarnings("MethodMayBeStatic")
public class MixinStackSizeRenderer {

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
