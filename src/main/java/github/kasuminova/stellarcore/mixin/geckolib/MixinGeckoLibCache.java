package github.kasuminova.stellarcore.mixin.geckolib;

import github.kasuminova.stellarcore.mixin.util.StellarCoreFileResourcePackAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.io.IOException;
import java.lang.reflect.Field;

@Mixin(value = GeckoLibCache.class, remap = false)
public abstract class MixinGeckoLibCache {

    @Redirect(
            method = "handleZipResourcePack",
            at = @At(value = "INVOKE", target = "Ljava/lang/reflect/Field;get(Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private Object stellar_core$openZipFile(final Field field, final Object obj) throws IllegalAccessException {
        if (obj instanceof StellarCoreFileResourcePackAccessor s) {
            try {
                return s.stellar_core$getResourcePackZipFile();
            } catch (IOException ignored) {
            }
        }
        return field.get(obj);
    }

}
