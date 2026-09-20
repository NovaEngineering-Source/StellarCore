package github.kasuminova.stellarcore.mixin.minecraft.selector;

import com.google.common.base.Predicate;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingIdentityHashMap;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.multipart.ICondition;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

@Mixin(value = Selector.class, remap = false)
public class MixinSelector {

    @Unique
    private static final Object STELLAR_CORE$NULL_PREDICATE = new Object();

    @Final
    @Shadow(remap = false)
    private ICondition condition;

    @Unique
    private final Map<BlockStateContainer, Object> stellar_core$predicateCache =
        new NonBlockingIdentityHashMap<>();

    /**
     * @author Circulate233
     * @reason Cache multipart selector predicates by state-container identity.
     */
    @SuppressWarnings("unchecked")
    @Overwrite(remap = false)
    public Predicate<IBlockState> getPredicate(final BlockStateContainer state) {
        if (!StellarCoreConfig.PERFORMANCE.vanilla.selectorPredicateCache) {
            return this.condition.getPredicate(state);
        }
        final Object cached = stellar_core$predicateCache.get(state);
        if (cached != null) {
            return cached == STELLAR_CORE$NULL_PREDICATE
                ? null
                : (Predicate<IBlockState>) cached;
        }
        final Predicate<IBlockState> computed = this.condition.getPredicate(state);
        stellar_core$predicateCache.putIfAbsent(
            state, computed == null ? STELLAR_CORE$NULL_PREDICATE : computed
        );
        return computed;
    }
}
