package github.kasuminova.stellarcore.mixin.ic2;

import ic2.core.recipe.GradualRecipe;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GradualRecipe.class)
public class MixinGradualRecipe {

    @Redirect(
            method = "getCraftingResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;copy()Lnet/minecraft/item/ItemStack;"
            )
    )
    public ItemStack redirectItemStackCopy(final ItemStack instance) {
        ItemStack copied = instance.copy();
        if (copied.getCount() > 1) {
            copied.setCount(1);
        }
        return copied;
    }

}
