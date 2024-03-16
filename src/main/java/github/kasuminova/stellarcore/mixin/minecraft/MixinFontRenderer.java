package github.kasuminova.stellarcore.mixin.minecraft;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(FontRenderer.class)
public class MixinFontRenderer {

    @Shadow @Final private int[] colorCode;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(final GameSettings gameSettingsIn, final ResourceLocation location, final TextureManager textureManagerIn, final boolean unicode, final CallbackInfo ci) {
        // 修改字体颜色喵
        colorCode[0] = 0x000000;
        colorCode[1] = 0x1e90ff;
        colorCode[2] = 0x00c853;
        colorCode[3] = 0x4db6ac;
        colorCode[4] = 0xd32f2f;
        colorCode[5] = 0xe040fb;
        colorCode[6] = 0xffa726;
        colorCode[7] = 0xbdbdbd;
        colorCode[8] = 0x546e7a;
        colorCode[9] = 0x03a9f4;
        colorCode[10] = 0x69f0ae;
        colorCode[11] = 0x18ffff;
        colorCode[12] = 0xff5e62;
        colorCode[13] = 0xff80ab;
        colorCode[14] = 0xffeb3b;
        colorCode[15] = 0xffffff;

        for (int i = 0; i < 16; i++) {
            Color color = new Color(colorCode[i]);
            Color backgroundColor = new Color(color.getRed() / 4, color.getGreen() / 4, color.getBlue() / 4);
            colorCode[i + 16] = backgroundColor.getRGB();
        }
    }

}
