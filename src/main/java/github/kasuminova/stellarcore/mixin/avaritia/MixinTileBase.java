package github.kasuminova.stellarcore.mixin.avaritia;

import morph.avaritia.tile.TileBase;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TileBase.class)
public class MixinTileBase extends TileEntity {

    /**
     * @author Kasumi_Nova
     * @reason 移除不必要的同步。
     */
    @Overwrite
    public void markDirty() {
        super.markDirty();
    }

}
