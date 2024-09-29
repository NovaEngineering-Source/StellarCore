package github.kasuminova.stellarcore.mixin.fluxnetworks;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sonar.fluxnetworks.api.energy.ITileEnergyHandler;
import sonar.fluxnetworks.common.connection.transfer.ConnectionTransfer;

@Mixin(value = ConnectionTransfer.class, remap = false)
public class MixinConnectionTransfer {

    @Shadow
    @Final
    private ITileEnergyHandler energyHandler;

    @Shadow
    @Final
    private TileEntity tile;

    @Shadow
    @Final
    private EnumFacing side;

    @Shadow
    public long inbound;

    @Unique
    private boolean stellar_core$canAddEnergy = false;

    /**
     * @author Kasumi_Nova
     * @reason Selective judgement
     */
    @Inject(method = "sendToTile", at = @At("HEAD"))
    public void sendToTile(final long amount, final boolean simulate, final CallbackInfoReturnable<Long> cir) {
        if (!StellarCoreConfig.PERFORMANCE.fluxNetworks.connectionTransfer) {
            return;
        }
        if (simulate) {
            if (energyHandler.canAddEnergy(tile, side)) {
                stellar_core$canAddEnergy = true;
                cir.setReturnValue(energyHandler.addEnergy(amount, tile, side, true));
                return;
            }
            stellar_core$canAddEnergy = false;
            cir.setReturnValue(0L);
            return;
        }

        if (stellar_core$canAddEnergy) {
            stellar_core$canAddEnergy = false;
            long added = energyHandler.addEnergy(amount, tile, side, false);
            inbound += added;
            cir.setReturnValue(added);
            return;
        }
        cir.setReturnValue(0L);
    }

}
