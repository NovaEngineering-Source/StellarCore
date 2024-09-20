package github.kasuminova.stellarcore.mixin.minecraft.blockstateimpl;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@SuppressWarnings("NonFinalFieldReferencedInHashCode")
@Mixin(BlockStateContainer.StateImplementation.class)
public class MixinStateImplementation {

    @Final
    @Shadow
    private ImmutableMap<IProperty<?>, Comparable<?>> properties;

    @Unique
    private int stellar_core$hashCode;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void injectInit() {
        this.stellar_core$hashCode = this.properties.hashCode();
    }

    /**
     * @author Kasumi_Nova
     * @reason hashCode Cache
     */
    @Overwrite
    public int hashCode() {
        return stellar_core$hashCode;
    }

}
