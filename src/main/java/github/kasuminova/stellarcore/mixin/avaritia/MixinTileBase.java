package github.kasuminova.stellarcore.mixin.avaritia;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import morph.avaritia.tile.TileBase;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileBase.class)
public class MixinTileBase extends TileEntity {

    /**
     * @author Kasumi_Nova
     * @reason 移除不必要的同步。
     */
    @Inject(method = "markDirty", at = @At("HEAD"), cancellable = true)
    public void markDirty(final CallbackInfo ci) {
        if (StellarCoreConfig.PERFORMANCE.avaritia.tileBase) {
            superMarkDirty();
            ci.cancel();
        }
    }

    @Unique
    private void superMarkDirty() {
        super.markDirty();
    }

}
