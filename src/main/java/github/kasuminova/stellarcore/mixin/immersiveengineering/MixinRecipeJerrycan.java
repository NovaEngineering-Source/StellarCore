package github.kasuminova.stellarcore.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.crafting.RecipeJerrycan;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RecipeJerrycan.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinRecipeJerrycan {

    @Redirect(
            method = "getRelevantSlots",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fluids/FluidUtil;getFluidHandler(Lnet/minecraft/item/ItemStack;)Lnet/minecraftforge/fluids/capability/IFluidHandlerItem;",
                    remap = false
            ),
            remap = false)
    private IFluidHandlerItem redirectGetRelevantSlotsGetFluidHandler(final ItemStack itemStack) {
        if (IEContent.itemJerrycan.equals(itemStack.getItem())) {
            return null;
        }
        return FluidUtil.getFluidHandler(itemStack);
    }

}
