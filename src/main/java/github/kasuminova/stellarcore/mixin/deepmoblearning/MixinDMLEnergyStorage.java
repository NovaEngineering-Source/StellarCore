package github.kasuminova.stellarcore.mixin.deepmoblearning;

import com.llamalad7.mixinextras.sugar.Local;
import mustapelto.deepmoblearning.common.energy.DMLEnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DMLEnergyStorage.class, remap = false)
public abstract class MixinDMLEnergyStorage {

    @Shadow
    protected abstract void onEnergyChanged();

    @Redirect(method = "receiveEnergy", at = @At(value = "INVOKE", target = "Lmustapelto/deepmoblearning/common/energy/DMLEnergyStorage;onEnergyChanged()V"))
    private void redirectReceiveEnergyOnEnergyChanged(final DMLEnergyStorage _inst, @Local(name = "simulate") boolean simulate) {
        if (!simulate) {
            // Prevent async chunk update.
            this.onEnergyChanged();
        }
    }

}
