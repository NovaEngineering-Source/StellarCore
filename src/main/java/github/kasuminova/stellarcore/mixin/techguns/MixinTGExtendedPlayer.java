package github.kasuminova.stellarcore.mixin.techguns;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import techguns.capabilities.TGExtendedPlayer;
import techguns.gui.player.TGPlayerInventory;

@Mixin(TGExtendedPlayer.class)
public class MixinTGExtendedPlayer {

    @Shadow(remap = false) public boolean enableSafemode;

    @Shadow(remap = false) public TGPlayerInventory tg_inventory;

    @Shadow(remap = false) public EntityPlayer entity;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void onInit(final EntityPlayer entity, final CallbackInfo ci) {
        if (!StellarCoreConfig.FEATURES.techguns.forceSecurityMode) {
            return;
        }
        enableSafemode = true;
    }

    @Inject(method = "copyFrom", at = @At("RETURN"), remap = false)
    private void onCopyFrom(final TGExtendedPlayer other, final CallbackInfo ci) {
        TGPlayerInventory newInv = new TGPlayerInventory(entity);
        NBTTagCompound nbt = new NBTTagCompound();
        other.tg_inventory.saveNBTData(nbt);
        newInv.loadNBTData(nbt);
        this.tg_inventory = newInv;
    }

}
