package github.kasuminova.stellarcore.mixin.avaritiaddons;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wanion.avaritiaddons.block.extremeautocrafter.TileEntityExtremeAutoCrafter;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(TileEntityExtremeAutoCrafter.class)
public class MixinTileEntityExtremeAutoCrafter {

    @Inject(method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;isEmpty()Z",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void injectInputStackIsEmpty(final CallbackInfo ci,
                                         @Local(name = "recipeStack") final ItemStack recipeStack,
                                         @Local(name = "outputStack") final ItemStack outputStack)
    {
        if (!StellarCoreConfig.BUG_FIXES.avaritaddons.tileEntityExtremeAutoCrafter) {
            return;
        }
        if (!recipeStack.isEmpty() && (!outputStack.isEmpty() && !recipeStack.isItemEqual(outputStack))) {
            ci.cancel();
        }
    }

}
