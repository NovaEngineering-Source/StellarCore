package github.kasuminova.stellarcore.mixin.enderutilities;

import fi.dy.masa.enderutilities.gui.client.base.GuiContainerLargeStacks;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(GuiContainerLargeStacks.class)
public class MixinGuiContainerLargeStacks {

    @ModifyConstant(
            method = "renderLargeStackItemOverlayIntoGUI",
            constant = @Constant(doubleValue = 0.5),
            remap = false
    )
    public double onRenderStackSize(final double ci) {
        return StellarCoreConfig.FEATURES.fontScale.enderUtilities;
    }

    @Redirect(
            method = "renderLargeStackItemOverlayIntoGUI",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I",
                    remap = true
            ),
            remap = false
    )
    public int onRenderStackSize(final FontRenderer instance, final String text, final float x, final float y, final int color) {
        float xOffset = Math.round(((16F / StellarCoreConfig.FEATURES.fontScale.enderUtilities) - instance.getStringWidth(text)));
        float yOffset = Math.round((16F / StellarCoreConfig.FEATURES.fontScale.enderUtilities) - 8);
        return instance.drawStringWithShadow(text, xOffset, yOffset, color);
    }

}
