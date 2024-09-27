package github.kasuminova.stellarcore.mixin.ic2;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import ic2.core.util.StackUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StackUtil.class, remap = false)
public class MixinStackUtil {

    @Redirect(
            method = "getAdjacentInventory",
            at = @At(
                    value = "INVOKE",
                    target = "Lic2/core/util/StackUtil;isInventoryTile(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)Z"
            )
    )
    private static boolean modifyGetAdjacentInventoryFacing(final TileEntity te, final EnumFacing side) {
        if (!StellarCoreConfig.BUG_FIXES.industrialCraft2.stackUtilInvFacingFixes) {
            return StackUtil.isInventoryTile(te, side);
        }
        return StackUtil.isInventoryTile(te, side.getOpposite());
    }

    @Inject(method = "isEmpty(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private static void injectIsEmpty(final ItemStack stack, final CallbackInfoReturnable<Boolean> cir) {
        if (stack == null || stack.isEmpty()) {
            cir.setReturnValue(true);
        }
    }

}
