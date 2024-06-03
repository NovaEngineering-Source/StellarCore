package github.kasuminova.stellarcore.mixin.enderio;

import com.enderio.core.common.TileEntityBase;
import com.enderio.core.common.config.ConfigHandler;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityBase.class)
public abstract class MixinTileEntityBase extends TileEntity {

    @Shadow(remap = false) private long lastUpdate;

    @Shadow(remap = false) protected abstract void doUpdate();

    @Shadow(remap = false) protected abstract void sendProgressIf();

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void injectUpdate(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.enderIO.tileEntityBase) {
            return;
        }
        if (this.world.getTileEntity(this.getPos()) == this) {
            long totalWorldTime = this.world.getTotalWorldTime();
            if (ConfigHandler.allowExternalTickSpeedup || totalWorldTime != this.lastUpdate) {
                this.lastUpdate = totalWorldTime;
                this.doUpdate();
                this.sendProgressIf();
            }
        }
        ci.cancel();
    }

}
