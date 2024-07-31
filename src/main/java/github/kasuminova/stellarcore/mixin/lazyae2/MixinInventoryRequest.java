package github.kasuminova.stellarcore.mixin.lazyae2;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import io.github.phantamanta44.threng.tile.TileLevelMaintainer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileLevelMaintainer.InventoryRequest.class, remap = false)
public class MixinInventoryRequest {

    @Final
    @Shadow
    private ItemStack[] requestStacks;

    @Final
    @Shadow
    private long[] requestQtys;

    @Final
    @Shadow
    private long[] requestBatches;

    @Inject(method = "computeDelta", at = @At("HEAD"), cancellable = true)
    private void injectComputeDelta(final int index, final long existing, final CallbackInfoReturnable<Long> cir) {
        if (!StellarCoreConfig.FEATURES.lazyAE2.levelMaintainerRequest) {
            return;
        }
        cir.setReturnValue(this.requestStacks[index].isEmpty() || this.requestQtys[index] - existing <= 0L ? 0L : this.requestBatches[index]);
    }

}
