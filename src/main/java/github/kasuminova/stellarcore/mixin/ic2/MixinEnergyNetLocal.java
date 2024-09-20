package github.kasuminova.stellarcore.mixin.ic2;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.BlockPos2ValueMap;
import ic2.core.energy.grid.EnergyNetLocal;
import ic2.core.energy.grid.Tile;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = EnergyNetLocal.class, remap = false)
public class MixinEnergyNetLocal {

    @Final
    @Shadow
    @Mutable
    Map<BlockPos, Tile> registeredTiles;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.industrialCraft2.energyNetLocal) {
            return;
        }
        this.registeredTiles = new BlockPos2ValueMap<>();
    }

}
