package github.kasuminova.stellarcore.client.handler;

import com.llamalad7.betterchat.gui.GuiBetterChat;
import github.kasuminova.stellarcore.client.hudcaching.HUDCaching;
import github.kasuminova.stellarcore.client.util.TitleUtils;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

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
    }

    @Optional.Method(modid = "betterchat")
    protected static void handleBetterChatAnim() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.hideGUI) {
            return;
        }
        GuiNewChat gui = mc.ingameGUI.getChatGUI();
        if (gui instanceof GuiBetterChat && GuiBetterChat.percentComplete < 1F && StellarCoreConfig.PERFORMANCE.vanilla.hudCaching) {
            HUDCaching.dirty = true;
        }
    }

}
