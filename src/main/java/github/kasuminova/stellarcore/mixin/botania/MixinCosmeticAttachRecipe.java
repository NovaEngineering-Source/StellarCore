package github.kasuminova.stellarcore.mixin.botania;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.common.crafting.recipe.CosmeticAttachRecipe;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(CosmeticAttachRecipe.class)
public class MixinCosmeticAttachRecipe {

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void removeMatches(final InventoryCrafting var1, final World var2, final CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "getCraftingResult", at = @At("HEAD"), cancellable = true)
    private void removeGetCraftingResult(final InventoryCrafting var1, final CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(ItemStack.EMPTY);
    }

}
