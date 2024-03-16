package github.kasuminova.stellarcore.mixin.legendarytooltips;

import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

@Mixin(TooltipDecor.class)
public class MixinTooltipDecor {

    /**
     * 换行有问题捏。
     */
    @Redirect(method = "drawBorder",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;listFormattedStringToWidth(Ljava/lang/String;I)Ljava/util/List;",
                    remap = true
            ),
            remap = false)
    private static List<String> redirectDrawBorderListFormattedStringToWidth(final FontRenderer instance, final String str, final int wrapWidth) {
        return Collections.singletonList(str);
    }

}
