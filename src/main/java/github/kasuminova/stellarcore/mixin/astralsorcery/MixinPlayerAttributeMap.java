package github.kasuminova.stellarcore.mixin.astralsorcery;

import com.google.common.collect.Lists;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import hellfirepvp.astralsorcery.common.constellation.perk.PlayerAttributeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
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
        if (!StellarCoreConfig.BUG_FIXES.astralSorcery.playerAttributeMap) {
            return map.computeIfAbsent(key, t -> new ArrayList<>());
        }
        return map.computeIfAbsent(key, t -> new CopyOnWriteArrayList<>());
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
        if (!StellarCoreConfig.BUG_FIXES.astralSorcery.playerAttributeMap) {
            return map.computeIfAbsent(key, t -> new ArrayList<>());
        }
        return map.computeIfAbsent(key, t -> new CopyOnWriteArrayList<>());
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
        if (!StellarCoreConfig.BUG_FIXES.astralSorcery.playerAttributeMap) {
            return map.computeIfAbsent(key, t -> new ArrayList<>());
        }
        return map.computeIfAbsent(key, t -> new CopyOnWriteArrayList<>());
    }

}
