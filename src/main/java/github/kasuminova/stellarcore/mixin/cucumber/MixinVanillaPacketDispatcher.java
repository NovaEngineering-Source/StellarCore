package github.kasuminova.stellarcore.mixin.cucumber;

import com.blakebr0.cucumber.util.VanillaPacketDispatcher;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(VanillaPacketDispatcher.class)
public class MixinVanillaPacketDispatcher {

    @ModifyConstant(method = "dispatchTEToNearbyPlayers(Lnet/minecraft/tileentity/TileEntity;)V", constant = @Constant(floatValue = 64F), remap = false)
    private static float modifyRange(final float constant) {
        if (!StellarCoreConfig.PERFORMANCE.cucumber.vanillaPacketDispatcher) {
            return constant;
        }
        return StellarCoreConfig.PERFORMANCE.cucumber.tileEntityUpdateRange;
    }

}
