package github.kasuminova.stellarcore.mixin.cfm;

import com.mrcrayfish.furniture.tileentity.TileEntityWashingMachine;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(TileEntityWashingMachine.class)
public class MixinTileEntityWashingMachine {

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 0))
    private boolean redirectUpdateIsEmpty(final ItemStack instance) {
        if (!StellarCoreConfig.BUG_FIXES.mrCrayfishFurniture.washingMachine) {
            return instance.isEmpty();
        }
        return instance.isEmpty() || !instance.getItem().isRepairable();
    }

}
