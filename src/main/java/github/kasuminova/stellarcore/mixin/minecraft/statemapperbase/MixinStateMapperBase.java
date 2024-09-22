package github.kasuminova.stellarcore.mixin.minecraft.statemapperbase;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(StateMapperBase.class)
public class MixinStateMapperBase {

    @Shadow
    protected Map<IBlockState, ModelResourceLocation> mapStateModelLocations;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void injectInit(final CallbackInfo ci) {
        if (StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader) {
            this.mapStateModelLocations = new ConcurrentHashMap<>();
            return;
        }
        this.mapStateModelLocations = new Object2ObjectLinkedOpenHashMap<>();
    }

}
