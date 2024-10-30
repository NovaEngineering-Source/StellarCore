package github.kasuminova.stellarcore.mixin.botania;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.common.block.tile.TileAlfPortal;
import vazkii.botania.common.block.tile.TileMod;

import java.util.List;

@Mixin(value = TileAlfPortal.class, remap = false)
public class MixinTileAlfPortal extends TileMod {

    @Unique
    private List<BlockPos> stellar_core$pylonCache = null;

    @Unique
    private long stellar_core$lastPylonCacheUpdate = 0;

    @Inject(method = "locatePylons", at = @At("HEAD"), cancellable = true)
    private void injectLocatePylons(final CallbackInfoReturnable<List<BlockPos>> cir) {
        if (!StellarCoreConfig.PERFORMANCE.botania.alfPortalImprovements) {
            return;
        }
        if (this.stellar_core$pylonCache != null && this.stellar_core$lastPylonCacheUpdate + 20 > this.world.getTotalWorldTime()) {
            cir.setReturnValue(this.stellar_core$pylonCache);
        }
        this.stellar_core$lastPylonCacheUpdate = this.world.getTotalWorldTime();
    }

    @ModifyReturnValue(method = "locatePylons", at = @At("RETURN"))
    private List<BlockPos> injectLocatePylonsReturn(final List<BlockPos> list) {
        this.stellar_core$pylonCache = list;
        return list;
    }

}
