package github.kasuminova.stellarcore.mixin.ic2;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import ic2.core.energy.grid.EnergyNetLocal;
import ic2.core.energy.grid.Grid;
import ic2.core.energy.grid.Node;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(Grid.class)
public class MixinGrid {

    @Final
    @Mutable
    @Shadow(remap = false)
    private Map<Integer, Node> nodes;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void injectInit(final EnergyNetLocal enet, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.industrialCraft2.grid) {
            return;
        }
        this.nodes = new Int2ObjectOpenHashMap<>();
    }

}
