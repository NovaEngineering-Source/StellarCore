package github.kasuminova.stellarcore.mixin.ic2;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.FastClearIdentityHashMap;
import ic2.core.energy.grid.Node;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(targets = "ic2.core.energy.leg.EnergyCalculatorLeg$GridData", remap = false)
public class MixinEnergyCalculatorLegGridData {

    @Final
    @Shadow
    @Mutable
    List<Node> activeSources;

    @Final
    @Shadow
    @Mutable
    Set<Object> eventPaths;

    @Final
    @Shadow
    @Mutable
    Map<Node, MutableDouble> activeSinks;

    @Inject(method = "<init>()V", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.industrialCraft2.energyCalculatorLegGridData) {
            return;
        }
        this.activeSources = new ObjectArrayList<>();
        this.eventPaths = new ReferenceOpenHashSet<>();
        this.activeSinks = new FastClearIdentityHashMap<>();
    }

}
