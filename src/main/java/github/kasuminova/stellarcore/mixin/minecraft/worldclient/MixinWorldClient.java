package github.kasuminova.stellarcore.mixin.minecraft.worldclient;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(WorldClient.class)
public class MixinWorldClient {

    @Inject(method = "invalidateRegionAndSetBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z"), cancellable = true)
    private void injectInvalidateRegionAndSetBlock(final BlockPos pos, final IBlockState state, final CallbackInfoReturnable<Boolean> cir) {
        if (state == null) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

}
