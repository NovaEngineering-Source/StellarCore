package github.kasuminova.stellarcore.mixin.modularrouters;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import me.desht.modularrouters.container.handler.BufferHandler;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.UniversalBucket;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = BufferHandler.class, remap = false)
public class MixinBufferHandler {

    @Redirect(
            method = "onContentsChanged",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fluids/FluidUtil;getFluidHandler(Lnet/minecraft/item/ItemStack;)Lnet/minecraftforge/fluids/capability/IFluidHandlerItem;"
            )
    )
    private IFluidHandlerItem redirectOnContentsChanged(final ItemStack itemStack) {
        if (!StellarCoreConfig.BUG_FIXES.modularRouters.bufferHandler) {
            return FluidUtil.getFluidHandler(itemStack);
        }

        Item item = itemStack.getItem();
        if (item == Items.BUCKET || item == Items.LAVA_BUCKET || item == Items.WATER_BUCKET || item == Items.MILK_BUCKET || item instanceof UniversalBucket) {
            return null;
        }

        IFluidHandlerItem handler = FluidUtil.getFluidHandler(itemStack);
        if (handler == null) {
            return null;
        }

        IFluidTankProperties[] prop = handler.getTankProperties();
        if (prop.length == 1) {
            FluidStack contents = prop[0].getContents();
            // 暴力测试.
            if (contents != null && contents.amount == 1000 && handler.drain(999, false) == null) {
                return null;
            }
        }

        return handler;
    }

}
