package github.kasuminova.stellarcore.mixin.util;

import ic2.core.energy.grid.Node;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AccessorGridData {

    boolean isActive();

    Map<Node, List<Object>> getEnergySourceToEnergyPathMap();

    List<Node> getActiveSources();

    Map<Node, MutableDouble> getActiveSinks();

    Set<Object> getEventPaths();

    Map<Node, List<Object>> getPathCache();

    int currentCalcId();

    int incrementCurrentCalcId();

}
