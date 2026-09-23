package github.kasuminova.stellarcore.mixin.minecraft.statemapperbase;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.StellarCoreStateMapper;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingIdentityHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@Mixin(StateMapperBase.class)
public abstract class MixinStateMapperBase implements StellarCoreStateMapper {

    @Unique
    private static final int STELLAR_CORE$MIN_CONCURRENT_CAP = 64;

    @Shadow
    protected Map<IBlockState, ModelResourceLocation> mapStateModelLocations;

    @Shadow
    protected abstract ModelResourceLocation getModelResourceLocation(IBlockState state);

    @Unique
    private boolean stellar_core$frozen;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void injectInit(final CallbackInfo ci) {
        if (StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader) {
            this.mapStateModelLocations = stellar_core$newConcurrent(STELLAR_CORE$MIN_CONCURRENT_CAP);
            return;
        }
        this.mapStateModelLocations = new Reference2ObjectOpenHashMap<>(STELLAR_CORE$MIN_CONCURRENT_CAP);
    }

    @Inject(method = "putStateModelLocations", at = @At("HEAD"), cancellable = true)
    private void stellar_core$putStateModelLocations(Block blockIn, CallbackInfoReturnable<Map<IBlockState, ModelResourceLocation>> cir) {
        final List<IBlockState> validStates = blockIn.getBlockState().getValidStates();
        if (StellarCoreConfig.PERFORMANCE.vanilla.parallelModelLoader
            && (!(this.mapStateModelLocations instanceof ConcurrentMap) || this.stellar_core$frozen)) {
            stellar_core$ensureConcurrent(Math.max(STELLAR_CORE$MIN_CONCURRENT_CAP, validStates.size()));
        }

        final Map<IBlockState, ModelResourceLocation> map = new Reference2ObjectOpenHashMap<>(validStates.size());

        if (this.mapStateModelLocations instanceof ConcurrentMap<IBlockState, ModelResourceLocation> chm) {
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

    @Override
    public void stellar_core$ensureConcurrent() {
        stellar_core$ensureConcurrent(Math.max(STELLAR_CORE$MIN_CONCURRENT_CAP, stellar_core$size()));
    }

    @Override
    public void stellar_core$freeze() {
        if (this.stellar_core$frozen) {
            return;
        }
        final Map<IBlockState, ModelResourceLocation> src = this.mapStateModelLocations;
        if (src == null || src.isEmpty()) {
            this.mapStateModelLocations = Collections.emptyMap();
            this.stellar_core$frozen = true;
            return;
        }
        final Reference2ObjectOpenHashMap<IBlockState, ModelResourceLocation> compact =
            new Reference2ObjectOpenHashMap<>(src.size());
        compact.putAll(src);
        compact.trim();
        this.mapStateModelLocations = Collections.unmodifiableMap(compact);
        this.stellar_core$frozen = true;
    }

    @Unique
    private void stellar_core$ensureConcurrent(final int expected) {
        if (this.mapStateModelLocations instanceof ConcurrentMap && !this.stellar_core$frozen) {
            return;
        }
        final NonBlockingIdentityHashMap<IBlockState, ModelResourceLocation> next =
            stellar_core$newConcurrent(expected);
        if (this.mapStateModelLocations != null && !this.mapStateModelLocations.isEmpty()) {
            next.putAll(this.mapStateModelLocations);
        }
        this.mapStateModelLocations = next;
        this.stellar_core$frozen = false;
    }

    @Unique
    private int stellar_core$size() {
        return this.mapStateModelLocations == null ? 0 : this.mapStateModelLocations.size();
    }

    @Unique
    private static NonBlockingIdentityHashMap<IBlockState, ModelResourceLocation> stellar_core$newConcurrent(final int expected) {
        return new NonBlockingIdentityHashMap<>(Math.max(STELLAR_CORE$MIN_CONCURRENT_CAP, expected));
    }

}
