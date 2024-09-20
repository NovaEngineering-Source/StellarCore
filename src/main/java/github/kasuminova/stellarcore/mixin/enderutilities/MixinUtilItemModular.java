package github.kasuminova.stellarcore.mixin.enderutilities;

import fi.dy.masa.enderutilities.util.nbt.NBTUtils;
import fi.dy.masa.enderutilities.util.nbt.UtilItemModular;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StackUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = UtilItemModular.class, remap = false)
public class MixinUtilItemModular {

    @Redirect(method = "getModuleStackBySlotNumber", at = @At(value = "INVOKE", target = "Lfi/dy/masa/enderutilities/util/nbt/NBTUtils;loadItemStackFromTag(Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/item/ItemStack;"))
    private static ItemStack injectGetModuleStackBySlotNumber(final NBTTagCompound moduleTag) {
        if (!StellarCoreConfig.PERFORMANCE.enderUtilities.utilItemModular) {
            return NBTUtils.loadItemStackFromTag(moduleTag);
        }
        return StackUtils.loadStackWithNonCap(moduleTag);
    }

}
