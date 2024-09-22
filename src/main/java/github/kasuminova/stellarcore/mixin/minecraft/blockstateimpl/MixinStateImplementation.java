package github.kasuminova.stellarcore.mixin.minecraft.blockstateimpl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("NonFinalFieldReferencedInHashCode")
@Mixin(BlockStateContainer.StateImplementation.class)
public class MixinStateImplementation {

    @Final
    @Shadow
    private ImmutableMap<IProperty<?>, Comparable<?>> properties;

    @Unique
    private int stellar_core$hashCode;

    @Inject(method = "<init>(Lnet/minecraft/block/Block;Lcom/google/common/collect/ImmutableMap;)V", at = @At("RETURN"))
    private void injectInit(final Block blockIn, final ImmutableMap<IProperty<?>, Comparable<?>> propertiesIn, final CallbackInfo ci) {
        this.stellar_core$hashCode = this.properties.hashCode();
    }

    @Inject(method = "<init>(Lnet/minecraft/block/Block;Lcom/google/common/collect/ImmutableMap;Lcom/google/common/collect/ImmutableTable;)V", at = @At("RETURN"))
    private void injectInit(final Block blockIn, final ImmutableMap<IProperty<?>, Comparable<?>> propertiesIn, final ImmutableTable<IProperty<?>, Comparable<?>, IBlockState> propertyValueTable, final CallbackInfo ci) {
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
