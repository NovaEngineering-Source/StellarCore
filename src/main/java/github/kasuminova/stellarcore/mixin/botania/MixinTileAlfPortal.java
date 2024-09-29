package github.kasuminova.stellarcore.mixin.botania;

import org.spongepowered.asm.mixin.Mixin;
import vazkii.botania.common.block.tile.TileAlfPortal;
import vazkii.botania.common.block.tile.TileMod;

@Mixin(value = TileAlfPortal.class, remap = false)
public class MixinTileAlfPortal extends TileMod {

}
