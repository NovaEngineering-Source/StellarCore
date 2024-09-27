package github.kasuminova.stellarcore.client.handler;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import com.cleanroommc.neverenoughanimations.NEAConfig;
import com.cleanroommc.neverenoughanimations.animations.HotbarAnimation;
import com.kamefrede.rpsideas.items.components.ItemBioticSensor;
import com.llamalad7.betterchat.gui.GuiBetterChat;
import com.windanesz.ancientspellcraft.client.entity.ASFakePlayer;
import github.kasuminova.stellarcore.client.hudcaching.HUDCaching;
import github.kasuminova.stellarcore.client.pool.ResourceLocationPool;
import github.kasuminova.stellarcore.client.pool.StellarUnpackedDataPool;
import github.kasuminova.stellarcore.client.util.TitleUtils;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.mod.Mods;
import github.kasuminova.stellarcore.common.util.StellarLog;
import journeymap.common.feature.PlayerRadarManager;
import mekanism.client.ClientTickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
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

        StellarUnpackedDataPool.update();

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

    @SubscribeEvent
    @SuppressWarnings("SpellCheckingInspection")
    public void onDebugText(RenderGameOverlayEvent.Text event) {
        if (!Minecraft.getMinecraft().gameSettings.showDebugInfo) {
            return;
        }

        List<String> left = event.getLeft();

        if (StellarCoreConfig.PERFORMANCE.forge.unpackedBakedQuadDataCanonicalization) {
            long unpackedProcessedCount = StellarUnpackedDataPool.getProcessedUnpackedDataCount();
            int unpackedUniqueCount = StellarUnpackedDataPool.getUnpackedDataUniqueCount();
            int canonicalizationLevel = StellarCoreConfig.PERFORMANCE.forge.unpackedBakedQuadDataCanonicalizationLevel;

            left.add("");
            left.add(String.format("%s<Stellar%sCore>%s: UnpackedData canonicalization level: %s%d",
                    Mods.RGB_CHAT.loaded() ? "#66CCFF-FF99CC" : TextFormatting.AQUA,
                    Mods.RGB_CHAT.loaded() ? ""               : TextFormatting.LIGHT_PURPLE,
                    TextFormatting.RESET,
                    canonicalizationLevel == 1 ? TextFormatting.GREEN
                            : canonicalizationLevel == 2 ? TextFormatting.GOLD
                            : TextFormatting.RED, // 3 = RED, 2 = GOLD, 1 = GREEN
                    canonicalizationLevel
            ));
            left.add(String.format("%s<Stellar%sCore>%s: %s%d%s UnpackedData processed. %s%d%s Unique, %s%d%s Deduplicated.",
                    Mods.RGB_CHAT.loaded() ? "#66CCFF-FF99CC" : TextFormatting.AQUA,
                    Mods.RGB_CHAT.loaded() ? ""               : TextFormatting.LIGHT_PURPLE,
                    TextFormatting.RESET,
                    TextFormatting.YELLOW, unpackedProcessedCount,                       TextFormatting.RESET,
                    TextFormatting.AQUA,   unpackedUniqueCount,                          TextFormatting.RESET,
                    TextFormatting.GREEN,  unpackedProcessedCount - unpackedUniqueCount, TextFormatting.RESET
            ));
        }

        if (StellarCoreConfig.PERFORMANCE.forge.unpackedBakedQuadVertexDataCanonicalization || StellarCoreConfig.PERFORMANCE.vanilla.bakedQuadVertexDataCanonicalization) {
            long vertexDataProcessedCount = StellarUnpackedDataPool.getProcessedVertexDataCount();
            int vertexDataUniqueCount = StellarUnpackedDataPool.getVertexDataUniqueCount();

            left.add(String.format("%s<Stellar%sCore>%s: %s%d%s VertexData processed. %s%d%s Unique, %s%d%s Deduplicated.",
                    Mods.RGB_CHAT.loaded() ? "#66CCFF-FF99CC" : TextFormatting.AQUA,
                    Mods.RGB_CHAT.loaded() ? ""               : TextFormatting.LIGHT_PURPLE,
                    TextFormatting.RESET,
                    TextFormatting.YELLOW, vertexDataProcessedCount,                         TextFormatting.RESET,
                    TextFormatting.AQUA,   vertexDataUniqueCount,                            TextFormatting.RESET,
                    TextFormatting.GREEN,  vertexDataProcessedCount - vertexDataUniqueCount, TextFormatting.RESET
            ));
        }

        if (StellarCoreConfig.PERFORMANCE.vanilla.resourceLocationCanonicalization) {
            long resourceLocationProcessedCount = ResourceLocationPool.INSTANCE.getProcessedCount();
            int resourceLocationUniqueCount = ResourceLocationPool.INSTANCE.getUniqueCount();

            left.add(String.format("%s<Stellar%sCore>%s: %s%d%s ResourceLocation Strings processed. %s%d%s Unique, %s%d%s Deduplicated.",
                    Mods.RGB_CHAT.loaded() ? "#66CCFF-FF99CC" : TextFormatting.AQUA,
                    Mods.RGB_CHAT.loaded() ? ""               : TextFormatting.LIGHT_PURPLE,
                    TextFormatting.RESET,
                    TextFormatting.YELLOW, resourceLocationProcessedCount,                  TextFormatting.RESET,
                    TextFormatting.AQUA,   resourceLocationUniqueCount,                     TextFormatting.RESET,
                    TextFormatting.GREEN,  resourceLocationProcessedCount - resourceLocationUniqueCount, TextFormatting.RESET
            ));
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
            StellarLog.LOG.warn("Failed to clear triggeredBioticsRemote in ItemBioticSensor.", e);
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
