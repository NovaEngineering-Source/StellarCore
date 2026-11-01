package github.kasuminova.stellarcore.mixin.libnine;

import io.github.phantamanta44.libnine.util.helper.ResourceUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.BufferedReader;
import java.io.Reader;

@Mixin(value = ResourceUtils.class, remap = false)
public abstract class MixinResourceUtils {

    @Redirect(method = "getAsJson", at = @At(value = "NEW", target = "java/io/BufferedReader"), require = 1)
    private static BufferedReader stellar_core$sizeJsonBuffer(final Reader in) {
        // Gson reads in 1024-character blocks and already buffers them. Matching that
        // size lets BufferedReader delegate bulk reads without an extra 8K buffer.
        return new BufferedReader(in, 1024);
    }
}
