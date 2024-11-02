package github.kasuminova.stellarcore.mixin.botania;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.BlockPos2ValueMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.block.tile.TilePylon;

import java.util.Map;

@Mixin(TilePylon.class)
public class MixinTilePylon {

    @Unique
    private final Map<BlockPos, IBlockState> stellar_core$stateCache = BlockPos2ValueMap.create();

    @Inject(method = "update", at = {@At("HEAD"), @At("RETURN")})
    private void injectUpdateStartAndEnd(final CallbackInfo ci) {
        stellar_core$stateCache.clear();
    }

    @Redirect(
            method = {"update", "getBlockForMeta", "portalOff"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"
            )
    )
    private IBlockState redirectGetBlockState(final World instance, final BlockPos pos) {
        if (!StellarCoreConfig.PERFORMANCE.botania.pylonImprovements) {
            return instance.getBlockState(pos);
        }
        return stellar_core$stateCache.computeIfAbsent(pos, instance::getBlockState);
    }

}
