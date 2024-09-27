package github.kasuminova.stellarcore.mixin.advancedrocketry;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zmaster587.advancedRocketry.api.SatelliteRegistry;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.item.ItemBiomeChanger;
import zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger;

@SuppressWarnings({"MethodMayBeStatic", "PatternVariableCanBeUsed"})
@Mixin(ItemBiomeChanger.class)
public class MixinItemBiomeChanger {

    @Redirect(
            method = "addInformation",
            at = @At(
                    value = "INVOKE",
                    target = "Lzmaster587/advancedRocketry/api/SatelliteRegistry;getSatellite(Lnet/minecraft/item/ItemStack;)Lzmaster587/advancedRocketry/api/satellite/SatelliteBase;",
                    remap = false
            )
    )
    private SatelliteBase wrapAddInformationNullCheck(final ItemStack stack) {
        if (!StellarCoreConfig.BUG_FIXES.advancedRocketry.itemBiomeChanger) {
            return SatelliteRegistry.getSatellite(stack);
        }

        SatelliteBase sat = SatelliteRegistry.getSatellite(stack);
        if (sat instanceof SatelliteBiomeChanger) {
            SatelliteBiomeChanger mapping = (SatelliteBiomeChanger) sat;
            if (mapping.getBiome() != null) {
                return mapping;
            }
        }
        return null;
    }

}
