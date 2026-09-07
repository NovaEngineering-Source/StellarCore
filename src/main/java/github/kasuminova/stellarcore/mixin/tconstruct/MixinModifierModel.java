package github.kasuminova.stellarcore.mixin.tconstruct;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.library.client.model.ModifierModel;

import java.util.Map;

@Mixin(value = ModifierModel.class, remap = false)
public class MixinModifierModel {

    @Shadow
    private Map<String, String> models;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void stellar_core$init(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader) {
            return;
        }
        this.models = new Object2ObjectOpenHashMap<>();
    }

}
