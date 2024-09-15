package github.kasuminova.stellarcore.mixin.tlm;

import com.github.tartaricacid.touhoulittlemaid.client.model.pojo.BonesItem;
import com.github.tartaricacid.touhoulittlemaid.client.model.pojo.CubesItem;
import github.kasuminova.stellarcore.client.pool.TLMCubesItemPool;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.CanonicalizationCubesItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = BonesItem.class, remap = false)
public class MixinBonesItem {

    @Shadow
    private List<Float> pivot;

    @Shadow
    private List<Float> rotation;

    @Shadow
    private List<CubesItem> cubes;

    @Unique
    private boolean stellar_core$canonicalized = false;

    @Inject(method = "getCubes", at = @At("HEAD"))
    private void injectGetCubes(final CallbackInfoReturnable<List<CubesItem>> cir) {
        stellar_core$canonicalize();
    }

    @Inject(method = "getPivot", at = @At("HEAD"))
    private void injectGetPivot(final CallbackInfoReturnable<List<Float>> cir) {
        stellar_core$canonicalize();
    }

    @Inject(method = "getRotation", at = @At("HEAD"))
    private void injectGetRotation(final CallbackInfoReturnable<List<Float>> cir) {
        stellar_core$canonicalize();
    }

    @Unique
    private void stellar_core$canonicalize() {
        if (!StellarCoreConfig.PERFORMANCE.tlm.modelDataCanonicalization) {
            return;
        }
        if (!stellar_core$canonicalized) {
            pivot = TLMCubesItemPool.canonicalize(pivot);
            rotation = TLMCubesItemPool.canonicalize(rotation);
            if (cubes != null) { // 我草，真有 null
                for (final CubesItem cube : cubes) {
                    ((CanonicalizationCubesItem) cube).stellar_core$canonicalize();
                }
            }
            stellar_core$canonicalized = true;
        }
    }

}
