package github.kasuminova.stellarcore.mixin.ic2;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import ic2.core.item.BaseElectricItem;
import ic2.core.item.ItemBattery;
import ic2.core.item.ItemIC2;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.model.ModelLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemBattery.class)
public class MixinItemBattery extends BaseElectricItem {

    @Shadow(remap = false)
    private static int maxLevel;

    public MixinItemBattery(final ItemName name, final double maxCharge, final double transferLimit, final int tier) {
        super(name, maxCharge, transferLimit, tier);
    }

    @Redirect(
            method = "registerModels",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/client/model/ModelLoader;setCustomMeshDefinition(Lnet/minecraft/item/Item;Lnet/minecraft/client/renderer/ItemMeshDefinition;)V",
                    remap = false
            ),
            remap = false
    )
    private void redirectRegisterModels(final Item item, final ItemMeshDefinition meshDefinition, final ItemName name) {
        ModelLoader.setCustomMeshDefinition(item, stack -> {
            if (!StellarCoreConfig.FEATURES.ic2.electricItemNonDurability) {
                return meshDefinition.getModelLocation(stack);
            }
            NBTTagCompound tNBT = stack.getTagCompound();
            double charge = tNBT == null ? 0D : tNBT.getDouble("charge");
            double maxCharge = getMaxCharge(stack);

            int level;
            if (maxCharge > 0) {
                level = (int) Math.round(Util.limit((charge / maxCharge) * maxLevel, 0, maxLevel));
            } else {
                level = 0;
            }

            return ItemIC2.getModelLocation(name, String.valueOf(level));
        });
    }

}
