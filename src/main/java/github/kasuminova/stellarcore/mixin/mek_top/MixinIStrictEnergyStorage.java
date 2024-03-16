package github.kasuminova.stellarcore.mixin.mek_top;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import mcjty.lib.api.power.IBigPower;
import mekanism.api.energy.IStrictEnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(IStrictEnergyStorage.class)
public interface MixinIStrictEnergyStorage extends IBigPower {

    @Shadow(remap = false) double getEnergy();

    @Shadow(remap = false) double getMaxEnergy();

    @Unique
    @Override
    default long getStoredPower() {
        double converted = getEnergy() / 2.5D;
        if (StellarCoreConfig.FEATURES.mekanism.topSupport) {
            return (long) Math.min(converted, Integer.MAX_VALUE);
        }
        if (converted >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.round(converted);
    }

    @Unique
    @Override
    default long getCapacity() {
        double converted = getMaxEnergy() / 2.5D;
        if (StellarCoreConfig.FEATURES.mekanism.topSupport) {
            return (long) Math.min(converted, Integer.MAX_VALUE);
        }
        if (converted >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.round(converted);
    }
}
