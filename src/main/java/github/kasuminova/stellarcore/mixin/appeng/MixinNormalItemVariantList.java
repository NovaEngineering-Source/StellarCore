package github.kasuminova.stellarcore.mixin.appeng;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "appeng.util.item.NormalItemVariantList", remap = false)
public abstract class MixinNormalItemVariantList {

    @Redirect(
        method = "<init>",
        at = @At(value = "NEW", target = "()Lit/unimi/dsi/fastutil/objects/Reference2ObjectOpenHashMap;"),
        remap = false
    )
    private Reference2ObjectOpenHashMap<?, ?> stellar_core$smallRecordMap() {
        if (!StellarCoreConfig.PERFORMANCE.appliedEnergistics.smallVariantRecordMap) {
            return new Reference2ObjectOpenHashMap<>();
        }
        return new Reference2ObjectOpenHashMap<>(2);
    }

}
