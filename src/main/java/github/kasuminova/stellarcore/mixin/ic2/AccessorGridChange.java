package github.kasuminova.stellarcore.mixin.ic2;

import ic2.api.energy.tile.IEnergyTile;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(targets = "ic2.core.energy.grid.GridChange", remap = false)
public interface AccessorGridChange {

    @Accessor
    BlockPos getPos();

    @Accessor
    IEnergyTile getIoTile();

    @Accessor
    List<IEnergyTile> getSubTiles();

}
