package github.kasuminova.stellarcore.mixin.astralsorcery;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import hellfirepvp.astralsorcery.common.constellation.perk.AbstractPerk;
import hellfirepvp.astralsorcery.common.constellation.perk.PerkConverter;
import hellfirepvp.astralsorcery.common.constellation.perk.PlayerAttributeMap;
import hellfirepvp.astralsorcery.common.constellation.perk.attribute.PerkAttributeModifier;
import hellfirepvp.astralsorcery.common.constellation.perk.attribute.PerkAttributeType;
import net.minecraftforge.fml.relauncher.Side;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@SuppressWarnings({"rawtypes", "unchecked", "MethodMayBeStatic"})
@Mixin(PlayerAttributeMap.class)
public class MixinPlayerAttributeMap {

    @Shadow(remap = false)
    private Map<PerkAttributeType, List<PerkAttributeModifier>> attributes;

    @Shadow(remap = false)
    private Set<AbstractPerk> cacheAppliedPerks;

    @Shadow(remap = false)
    private List<PerkConverter> converters;

    @Inject(
            method = "<init>",
            at = @At("RETURN"),
            remap = false
    )
    public void injectInit(final Side side, final CallbackInfo ci) {
        if (!StellarCoreConfig.BUG_FIXES.astralSorcery.playerAttributeMap) {
            return;
        }
        attributes = new ConcurrentHashMap<>();
        cacheAppliedPerks = Collections.newSetFromMap(new ConcurrentHashMap<>());
        converters = Collections.synchronizedList(new LinkedList<>());
    }

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
