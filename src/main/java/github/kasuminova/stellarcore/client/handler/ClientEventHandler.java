package github.kasuminova.stellarcore.client.handler;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import com.cleanroommc.neverenoughanimations.NEAConfig;
import com.cleanroommc.neverenoughanimations.animations.HotbarAnimation;
import com.kamefrede.rpsideas.items.components.ItemBioticSensor;
import com.llamalad7.betterchat.gui.GuiBetterChat;
import com.windanesz.ancientspellcraft.client.entity.ASFakePlayer;
import github.kasuminova.stellarcore.StellarCore;
import github.kasuminova.stellarcore.client.hudcaching.HUDCaching;
import github.kasuminova.stellarcore.client.util.TitleUtils;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import journeymap.common.feature.PlayerRadarManager;
import mekanism.client.ClientTickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import vazkii.botania.common.core.handler.ManaNetworkHandler;
import zmaster587.libVulpes.util.InputSyncHandler;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@SuppressWarnings("MethodMayBeStatic")
public class ClientEventHandler {
    public static final ClientEventHandler INSTANCE = new ClientEventHandler();

    private long clientTick = 0;

    private ClientEventHandler() {
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        clientTick++;

        if (clientTick % 5 == 0) {
            TitleUtils.checkTitleState();
        }
    }

    @SubscribeEvent
    public void onClientRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (Loader.isModLoaded("betterchat")) {
            handleBetterChatAnim();
        }
        if (Loader.isModLoaded("neverenoughanimations")) {
            handleNEAAnim();
        }
    }

    /**
     * Called by github.kasuminova.stellarcore.mixin.minecraft.world_load.MixinMinecraft<br/>
     * 修复各种奇怪的模组导致 WorldClient 无法被 GC 导致内存泄露的问题。
     */
    public void onClientWorldLoad(@Nullable final World world) {
        if (Loader.isModLoaded("ancientspellcraft")) {
            callAncientSpellCraftFakePlayerWorldChanged(world);
        }
        if (Loader.isModLoaded("libvulpes")) {
            callLibVulpesWorldChanged();
        }
        if (Loader.isModLoaded("mekanism")) {
            callMekanismClientTickHandlerWorldChanged();
        }
        if (Loader.isModLoaded("botania")) {
            callBotaniaManaNetworkHandlerWorldChanged();
        }
        if (Loader.isModLoaded("rpsideas")) {
            callRPSIdeasItemBioticSensorWorldChanged();
        }
        if (Loader.isModLoaded("journeymap")) {
            callJourneyMapPlayerRadarManagerWorldChanged();
        }
        if (Loader.isModLoaded("immersiveengineering")) {
            callImmersiveEngineeringWorldChanged();
        }
    }

    @Optional.Method(modid = "immersiveengineering")
    private static void callImmersiveEngineeringWorldChanged() {
        if (!StellarCoreConfig.BUG_FIXES.immersiveEngineering.renderCache) {
            return;
        }
        ImmersiveEngineering.proxy.clearRenderCaches();
    }

    @Optional.Method(modid = "journeymap")
    private static void callJourneyMapPlayerRadarManagerWorldChanged() {
        if (!StellarCoreConfig.BUG_FIXES.journeyMap.playerRadar) {
            return;
        }
        PlayerRadarManager.getInstance().clearNetworkPlayers();
    }

    @Optional.Method(modid = "rpsideas")
    private static void callRPSIdeasItemBioticSensorWorldChanged() {
        if (!StellarCoreConfig.BUG_FIXES.rpsIdeas.itemBioticSensor) {
            return;
        }
        try {
            Field triggeredBioticsRemote = ItemBioticSensor.class.getDeclaredField("triggeredBioticsRemote");
            triggeredBioticsRemote.setAccessible(true);
            ((Map<EntityPlayer, List<EntityLivingBase>>) triggeredBioticsRemote.get(null)).clear();
        } catch (Throwable e) {
            StellarCore.log.warn("Failed to clear triggeredBioticsRemote in ItemBioticSensor.", e);
        }
    }

    @Optional.Method(modid = "botania")
    private static void callBotaniaManaNetworkHandlerWorldChanged() {
        if (!StellarCoreConfig.BUG_FIXES.botania.manaNetworkHandler) {
            return;
        }
        ManaNetworkHandler.instance.clear();
    }

    @Optional.Method(modid = "mekanism")
    private static void callMekanismClientTickHandlerWorldChanged() {
        if (!StellarCoreConfig.BUG_FIXES.mekanism.portableTeleports) {
            return;
        }
        ClientTickHandler.portableTeleports.remove(Minecraft.getMinecraft().player);
    }

    @Optional.Method(modid = "ancientspellcraft")
    private static void callAncientSpellCraftFakePlayerWorldChanged(final World world) {
        if (!StellarCoreConfig.BUG_FIXES.ancientSpellCraft.asFakePlayer) {
            return;
        }
        ASFakePlayer.FAKE_PLAYER.setWorld(world);
    }

    @Optional.Method(modid = "libvulpes")
    private static void callLibVulpesWorldChanged() {
        if (!StellarCoreConfig.BUG_FIXES.libVulpes.inputSyncHandler) {
            return;
        }
        InputSyncHandler.spaceDown.remove(Minecraft.getMinecraft().player);
    }

    @Optional.Method(modid = "betterchat")
    private static void handleBetterChatAnim() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.hideGUI) {
            return;
        }
        GuiNewChat gui = mc.ingameGUI.getChatGUI();
        if (gui instanceof GuiBetterChat && GuiBetterChat.percentComplete < 1F && StellarCoreConfig.PERFORMANCE.vanilla.hudCaching) {
            HUDCaching.dirty = true;
        }
    }

    @Optional.Method(modid = "neverenoughanimations")
    private static void handleNEAAnim() {
        if (NEAConfig.hotbarAnimationTime != 0 && HotbarAnimation.isAnimationInProgress()) {
            HUDCaching.dirty = true;
        }
    }

}
