package github.kasuminova.stellarcore.mixin.astralsorcery;

import github.kasuminova.stellarcore.StellarCore;
import hellfirepvp.astralsorcery.common.constellation.perk.AbstractPerk;
import hellfirepvp.astralsorcery.common.constellation.perk.PerkEffectHelper;
import hellfirepvp.astralsorcery.common.data.research.PlayerProgress;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 尝试兼容 PlayerDataSQL 的申必还原机制导致服务器崩溃的问题。
 */
@Mixin(PlayerProgress.class)
public abstract class MixinPlayerProgress {
    @Shadow(remap = false) public abstract Collection<AbstractPerk> getAppliedPerks();

    @Shadow(remap = false) public abstract List<AbstractPerk> getSealedPerks();

    @Inject(method = "load", at = @At("HEAD"), remap = false)
    private void onLoadPre(final NBTTagCompound compound, final CallbackInfo ci) {
        EntityPlayerMP player = stellarcore$getCurrentPlayer();
        if (player == null) {
            return;
        }

        StellarCore.log.info("Try to removing all perk data for player " + player.getGameProfile().getName() + ".");
        try {
            for (final AbstractPerk perk : getAppliedPerks()) {
                ((InvokerPerkEffectHelper) PerkEffectHelper.EVENT_INSTANCE).invokeHandlePerkRemoval(perk, player, Side.SERVER);
            }
            for (final AbstractPerk perk : getSealedPerks()) {
                ((InvokerPerkEffectHelper) PerkEffectHelper.EVENT_INSTANCE).invokeHandlePerkRemoval(perk, player, Side.SERVER);
            }
        } catch (Throwable e) {
            StellarCore.log.warn("Remove failed!", e);
        }
    }

    @Inject(method = "load", at = @At("TAIL"), remap = false)
    private void onLoadPost(final NBTTagCompound compound, final CallbackInfo ci) {
        EntityPlayerMP player = stellarcore$getCurrentPlayer();
        if (player == null) {
            return;
        }

        StellarCore.log.info("Try to restoring new perk data for player " + player.getGameProfile().getName() + ".");
        try {
            ((InvokerPerkEffectHelper) PerkEffectHelper.EVENT_INSTANCE).invokeHandlePerkModification(player, Side.SERVER, false);
        } catch (Throwable e) {
            StellarCore.log.warn("Restore failed!", e);
        }
    }

    @Unique
    @Nullable
    private EntityPlayerMP stellarcore$getCurrentPlayer() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return null;
        }
        Map<UUID, PlayerProgress> playerProgress = AccessorResearchManager.getPlayerProgressServer();

        UUID playerUUID = null;
        for (final Map.Entry<UUID, PlayerProgress> entry : playerProgress.entrySet()) {
            if (entry.getValue() == (Object) this) {
                playerUUID = entry.getKey();
            }
        }
        //noinspection ConstantValue
        if (playerUUID == null) {
            return null;
        }
        return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(playerUUID);
    }

}
