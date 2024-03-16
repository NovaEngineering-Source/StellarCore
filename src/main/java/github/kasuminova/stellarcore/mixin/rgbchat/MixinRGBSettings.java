package github.kasuminova.stellarcore.mixin.rgbchat;

import com.fred.jianghun.truergb.IColor;
import com.fred.jianghun.truergb.RGBSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(RGBSettings.class)
public interface MixinRGBSettings {

    @Accessor(remap = false)
    List<IColor> getColors();

}
