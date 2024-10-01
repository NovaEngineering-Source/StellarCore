package github.kasuminova.stellarcore.mixin.minecraft.resourcelocation;

import github.kasuminova.stellarcore.common.pool.ResourceLocationPool;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Locale;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(ResourceLocation.class)
public class MixinResourceLocation {

    @Redirect(method = "<init>(I[Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Ljava/lang/String;toLowerCase(Ljava/util/Locale;)Ljava/lang/String;"))
    private String injectInit(final String instance, final Locale locale) {
        return ResourceLocationPool.INSTANCE.canonicalize(instance);
    }

}
