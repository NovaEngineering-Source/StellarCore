package github.kasuminova.stellarcore.mixin.ic2;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import ic2.core.util.StackUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StackUtil.class)
public class MixinStackUtil {

    @Redirect(method = "getAdjacentInventory",
            at = @At(
                    value = "INVOKE",
                    target = "Lic2/core/util/StackUtil;isInventoryTile(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)Z",
                    remap = false
            ),
            remap = false
    )
    private static boolean modifyGetAdjacentInventoryFacing(final TileEntity te, final EnumFacing side) {
        if (!StellarCoreConfig.BUG_FIXES.industrialCraft2.stackUtilInvFacingFixes) {
            return StackUtil.isInventoryTile(te, side);
        }
        return StackUtil.isInventoryTile(te, side.getOpposite());
    }

}
