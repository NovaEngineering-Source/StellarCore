package github.kasuminova.stellarcore.mixin.ic2_energynet;

import github.kasuminova.stellarcore.mixin.util.AccessorGridData;
import ic2.core.energy.grid.Node;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"AddedMixinMembersNamePattern", "unchecked", "rawtypes"})
@Mixin(targets = "ic2.core.energy.leg.EnergyCalculatorLeg$GridData", remap = false)
public class MixinGridData implements AccessorGridData {

    @Shadow
    boolean active;

    @Shadow
    @Final
    Map energySourceToEnergyPathMap;

    @Shadow
    @Final
    List<Node> activeSources;

    @Shadow
    @Final
    Map<Node, MutableDouble> activeSinks;

    @Shadow
    @Final
    Set eventPaths;

    @Shadow
    @Final
    Map pathCache;

    @Shadow
    int currentCalcId;

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Node, List<Object>> getEnergySourceToEnergyPathMap() {
        return (Map<Node, List<Object>>) (Map<?, ?>) energySourceToEnergyPathMap;
    }

    @Override
    public List<Node> getActiveSources() {
        return activeSources;
    }

    @Override
    public Map<Node, MutableDouble> getActiveSinks() {
        return activeSinks;
    }

    @Override
    public Set<Object> getEventPaths() {
        return (Set<Object>) eventPaths;
    }

    @Override
    public Map<Node, List<Object>> getPathCache() {
        return (Map<Node, List<Object>>) pathCache;
    }

    @Override
    public int currentCalcId() {
        return currentCalcId;
    }

    @Override
    public int incrementCurrentCalcId() {
        ++currentCalcId;
        return currentCalcId;
    }

}
