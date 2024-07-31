package github.kasuminova.stellarcore.mixin.ctm;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.client.model.parsing.ModelLoaderCTM;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = ModelLoaderCTM.class, remap = false)
public class MixinModelLoaderCTM {

    @Shadow
    private Map<ResourceLocation, IModelCTM> loadedModels;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final String par1, final int par2, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader) {
            return;
        }
        loadedModels = new ConcurrentHashMap<>();
    }

}
