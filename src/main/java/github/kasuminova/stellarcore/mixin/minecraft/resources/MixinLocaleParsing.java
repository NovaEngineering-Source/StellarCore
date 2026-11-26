package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.common.util.LanguageLineParser;
import net.minecraft.client.resources.Locale;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Mixin(Locale.class)
public abstract class MixinLocaleParsing {
    @Shadow
    Map<String, String> properties;

    /**
     * @author StellarCore
     * @reason Preserve Forge's language hook and atomic reads without a list and String per input line.
     */
    @Overwrite
    private void loadLocaleData(InputStream stream) throws IOException {
        stream = FMLCommonHandler.instance().loadLanguage(properties, stream);
        if (stream != null) {
            LanguageLineParser.read(properties, stream);
        }
    }
}
