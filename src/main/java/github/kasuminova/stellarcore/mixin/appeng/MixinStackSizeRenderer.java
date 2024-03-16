package github.kasuminova.stellarcore.mixin.appeng;

import appeng.client.render.StackSizeRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 优化当安装了 SmoothFont 时物品数量渲染字体过小的问题。
 */
@Mixin(StackSizeRenderer.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinStackSizeRenderer {

    @ModifyConstant(
            method = "renderStackSize",
            constant = {@Constant(floatValue = 0.85F), @Constant(floatValue = 0.5F)},
            remap = false
    )
    public float onRenderStackSize(final float ci) {
        if (ci == 0.5F) {
            return 0.7F;
        }
        return ci;
    }

    @ModifyConstant(
            method = "renderStackSize",
            constant = {@Constant(intValue = 0), @Constant(intValue = -1)},
            remap = false
    )
    public int onRenderStackSize(final int ci) {
        if (ci == -1) {
            return 0;
        }
        return ci;
    }

}
