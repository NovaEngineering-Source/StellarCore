package github.kasuminova.stellarcore.mixin.minecraft.forge;

import github.kasuminova.stellarcore.common.util.LanguageLineParser;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

@Mixin(value = FMLCommonHandler.class, remap = false)
public abstract class MixinFMLLanguageLoading {
    /**
     * @author StellarCore
     * @reason Check the already buffered file for Forge's directive without decoding every line first.
     */
    @Overwrite
    public InputStream loadLanguage(final Map<String, String> properties, final InputStream stream) throws IOException {
        final byte[] bytes = IOUtils.toByteArray(stream);
        final ByteArrayInputStream buffer = new ByteArrayInputStream(bytes);
        if (!LanguageLineParser.usesPropertyEscapes(bytes)) return buffer;

        final Properties escaped = new Properties();
        escaped.load(new InputStreamReader(buffer, StandardCharsets.UTF_8));
        for (final Map.Entry<Object, Object> entry : escaped.entrySet()) {
            properties.put((String) entry.getKey(), (String) entry.getValue());
        }
        escaped.clear();
        return null;
    }
}
