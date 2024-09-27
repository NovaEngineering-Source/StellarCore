package github.kasuminova.stellarcore.mixin.astralsorcery;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import hellfirepvp.astralsorcery.common.constellation.perk.tree.nodes.key.KeyChainMining;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyChainMining.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinKeyChainMining {

    /**
     * @author Kasumi_Nova
     * @reason 配置文件不让禁用，那就把方法爆了。
     */
    @Inject(method = "onBreak", at = @At("HEAD"), cancellable = true, remap = false)
    public void onBreak(final CallbackInfo ci) {
        if (StellarCoreConfig.FEATURES.astralSorcery.disableChainMining) {
            ci.cancel();
        }
    }

}
