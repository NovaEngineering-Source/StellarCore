package github.kasuminova.stellarcore.mixin.minecraft.statemapperbase;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingIdentityHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@Mixin(StateMapperBase.class)
public abstract class MixinStateMapperBase {

    @Shadow
    protected Map<IBlockState, ModelResourceLocation> mapStateModelLocations;

    @Shadow
    protected abstract ModelResourceLocation getModelResourceLocation(IBlockState state);

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void injectInit(final CallbackInfo ci) {
        if (StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader) {
            // Pre-size to reduce resizing/rehash overhead during parallel model loading.
            // Use identity map to avoid IBlockState.hashCode()/equals() costs.
            this.mapStateModelLocations = new NonBlockingIdentityHashMap<>(8192);
            return;
        }
        this.mapStateModelLocations = new Reference2ObjectOpenHashMap<>();
    }

    @Inject(method = "putStateModelLocations", at = @At("HEAD"), cancellable = true)
    private void stellar_core$putStateModelLocations(Block blockIn, CallbackInfoReturnable<Map<IBlockState, ModelResourceLocation>> cir) {
        final List<IBlockState> validStates = blockIn.getBlockState().getValidStates();
        final Map<IBlockState, ModelResourceLocation> map = new Reference2ObjectOpenHashMap<>(validStates.size());

        if (this.mapStateModelLocations instanceof ConcurrentMap) {
            @SuppressWarnings("unchecked")
            final ConcurrentMap<IBlockState, ModelResourceLocation> chm = (ConcurrentMap<IBlockState, ModelResourceLocation>) this.mapStateModelLocations;
            for (IBlockState state : validStates) {
                ModelResourceLocation location = chm.get(state);
                if (location == null) {
                    location = this.getModelResourceLocation(state);
                    final ModelResourceLocation existing = chm.putIfAbsent(state, location);
                    if (existing != null) {
                        location = existing;
                    }
                }
                map.put(state, location);
            }
            cir.setReturnValue(map);
            return;
        }

        for (IBlockState state : validStates) {
            final ModelResourceLocation location = this.mapStateModelLocations.computeIfAbsent(state, this::getModelResourceLocation);
            map.put(state, location);
        }
        cir.setReturnValue(map);
    }

}
