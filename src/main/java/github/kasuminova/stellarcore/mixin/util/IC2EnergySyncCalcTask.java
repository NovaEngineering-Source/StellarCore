package github.kasuminova.stellarcore.mixin.util;

import com.github.bsideup.jabel.Desugar;
import ic2.core.energy.grid.Grid;
import ic2.core.energy.grid.Node;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.util.List;
import java.util.Map;

@Desugar
public record IC2EnergySyncCalcTask(World world, int calcID, Grid grid, AccessorGridData gridData, List<Node> activeSources, Map<Node, MutableDouble> activeSinks) {

    public static final IC2EnergySyncCalcTask EMPTY = new IC2EnergySyncCalcTask(null, 0, null, null, null, null);

}
