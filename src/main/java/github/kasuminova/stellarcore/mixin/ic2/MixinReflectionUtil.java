package github.kasuminova.stellarcore.mixin.ic2;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import ic2.core.util.ReflectionUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

@Mixin(value = ReflectionUtil.class, remap = false)
public class MixinReflectionUtil {

    @Unique
    private static final Map<Class<?>, Map<String, Optional<Field>>> STELLAR_CORE$FIELD_CACHE = new Reference2ObjectOpenHashMap<>();

    @Unique
    private static final ThreadLocal<Class<?>> STELLAR_CORE$CURRENT_SEARCH_CLASS = new ThreadLocal<>();

    @Inject(method = "getFieldRecursive(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;", at = @At("HEAD"), cancellable = true)
    private static void injectGetFieldRecursive(final Class<?> clazz, final String fieldName, final CallbackInfoReturnable<Field> cir) {
        Optional<Field> cached = STELLAR_CORE$FIELD_CACHE.computeIfAbsent(clazz, (key) -> new Object2ObjectOpenHashMap<>()).get(fieldName);
        if (cached != null) {
            cir.setReturnValue(cached.orElse(null));
        } else {
            STELLAR_CORE$CURRENT_SEARCH_CLASS.set(clazz);
        }
    }

    @ModifyReturnValue(method = "getFieldRecursive(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;", at = @At("TAIL"))
    private static Field injectGetFieldRecursiveReturn(final Field ret, final Class<?> clazz, final String fieldName) {
        STELLAR_CORE$FIELD_CACHE.computeIfAbsent(STELLAR_CORE$CURRENT_SEARCH_CLASS.get(), (key) -> new Object2ObjectOpenHashMap<>()).put(fieldName, Optional.ofNullable(ret));
        STELLAR_CORE$CURRENT_SEARCH_CLASS.remove();
        return ret;
    }

}
