package github.kasuminova.stellarcore.mixin.util;

import com.github.bsideup.jabel.Desugar;
import crazypants.enderio.base.power.IPowerInterface;
import github.kasuminova.stellarcore.mixin.enderioconduits_energy.AccessorReceptorEntry;

@Desugar
public record ReceptorPowerInterface(IPowerInterface pp, AccessorReceptorEntry receptor) {
}
