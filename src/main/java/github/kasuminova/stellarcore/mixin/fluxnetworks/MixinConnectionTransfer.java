package github.kasuminova.stellarcore.mixin.fluxnetworks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.*;
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
    @Overwrite
    public long sendToTile(long amount, boolean simulate) {
        if (simulate) {
            if (energyHandler.canAddEnergy(tile, side)) {
                stellar_core$canAddEnergy = true;
                return energyHandler.addEnergy(amount, tile, side, true);
            }
            stellar_core$canAddEnergy = false;
            return 0;
        }

        if (stellar_core$canAddEnergy) {
            stellar_core$canAddEnergy = false;
            long added = energyHandler.addEnergy(amount, tile, side, false);
            inbound += added;
            return added;
        }
        return 0;
    }

}
