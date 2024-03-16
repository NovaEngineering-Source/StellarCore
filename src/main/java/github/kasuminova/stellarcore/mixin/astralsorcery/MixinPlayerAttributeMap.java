package github.kasuminova.stellarcore.mixin.astralsorcery;

import com.google.common.collect.Lists;
import hellfirepvp.astralsorcery.common.constellation.perk.PlayerAttributeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.function.Function;

@SuppressWarnings({"rawtypes", "unchecked", "MethodMayBeStatic"})
@Mixin(PlayerAttributeMap.class)
public class MixinPlayerAttributeMap {

    @Redirect(
            method = "applyModifier",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;",
                    remap = false
            ),
            remap = false)
    public Object redirectApplyModifierComputeIfAbsent(final Map map, final Object key, final Function _f) {
        return map.computeIfAbsent(key, t -> Lists.newCopyOnWriteArrayList());
    }

    @Redirect(
            method = "removeModifier",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;",
                    remap = false
            ),
            remap = false)
    public Object redirectRemoveModifierComputeIfAbsent(final Map map, final Object key, final Function _f) {
        return map.computeIfAbsent(key, t -> Lists.newCopyOnWriteArrayList());
    }

    @Redirect(
            method = "getModifiersByType",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;",
                    remap = false
            ),
            remap = false)
    public Object redirectGetModifiersByTypeComputeIfAbsent(final Map map, final Object key, final Function _f) {
        return map.computeIfAbsent(key, t -> Lists.newCopyOnWriteArrayList());
    }

}
