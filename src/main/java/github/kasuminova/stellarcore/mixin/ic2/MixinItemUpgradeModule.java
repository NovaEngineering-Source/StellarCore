package github.kasuminova.stellarcore.mixin.ic2;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.core.item.upgrade.ItemUpgradeModule;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(ItemUpgradeModule.class)
public class MixinItemUpgradeModule {

    @Inject(
            method = "onTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lic2/core/util/StackUtil;getSize(Lnet/minecraft/item/ItemStack;)I",
                    remap = false),
            remap = false,
            cancellable = true
    )
    private void injectOnTick(final ItemStack stack, final IUpgradableBlock parent, final CallbackInfoReturnable<Boolean> cir) {
        TileEntity te = (TileEntity) parent;
        if (te.getWorld().getTotalWorldTime() % 5 != 0) {
            cir.setReturnValue(false);
        }
    }

}
