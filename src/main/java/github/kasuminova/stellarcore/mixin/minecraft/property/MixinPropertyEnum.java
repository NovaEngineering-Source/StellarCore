package github.kasuminova.stellarcore.mixin.minecraft.property;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.properties.PropertyEnum;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Map;

@Mixin(PropertyEnum.class)
public class MixinPropertyEnum {

    @Final
    @Shadow
    private ImmutableSet<?> allowedValues;

    @Final
    @Shadow
    private Map<String, ?> nameToValue;

    @Unique
    private int stellar_core$cachedHashCode = 0;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final String name, final Class<?> valueClass, final Collection<?> allowedValues, final CallbackInfo ci) {
        int i = super.hashCode();
        i = 31 * i + this.allowedValues.hashCode();
        i = 31 * i + this.nameToValue.hashCode();
        stellar_core$cachedHashCode = i;
    }

    /**
     * @author Kasumi_Nova
     * @reason HashCode Cache
     */
    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Overwrite
    public int hashCode() {
        return stellar_core$cachedHashCode;
    }

}
