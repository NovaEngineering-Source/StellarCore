package github.kasuminova.stellarcore.mixin.cofhcore;

import cofh.core.block.TileInventory;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TileInventory.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinTileInventory {

    @Redirect(method = "extractItem",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;min(II)I",
                    ordinal = 1,
                    remap = false
            ),
            remap = false
    )
    private int injectExtractItem(final int a, final int b, @Local(name = "queryStack") ItemStack queryStack) {
        if (!StellarCoreConfig.BUG_FIXES.coFHCore.tileInventory) {
            return Math.min(a, b);
        }
        return Math.min(a, Math.min(b, queryStack.getMaxStackSize()));
    }

}
