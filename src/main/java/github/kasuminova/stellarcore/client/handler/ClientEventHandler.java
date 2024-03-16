package github.kasuminova.stellarcore.client.handler;

import github.kasuminova.stellarcore.StellarCore;
import github.kasuminova.stellarcore.client.profiler.PacketProfiler;
import github.kasuminova.stellarcore.client.profiler.TEUpdatePacketProfiler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

public class ClientEventHandler {
    public static final ClientEventHandler INSTANCE = new ClientEventHandler();

    public static int debugPacketProfilerMessageLimit = 5;
    public static int debugTEPacketProfilerMessageLimit = 5;

    private long clientTick = 0;

    private final List<String> debugMessageCache = new ArrayList<>();
    private boolean debugMessageUpdateRequired = true;

    private ClientEventHandler() {
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        clientTick++;

        if (clientTick % 5 == 0) {
            debugMessageUpdateRequired = true;
        }
    }

    @SubscribeEvent
    public void onDebugText(RenderGameOverlayEvent.Text event) {
        if (!Minecraft.getMinecraft().gameSettings.showDebugInfo) {
            return;
        }

        if (debugMessageUpdateRequired) {
            debugMessageUpdateRequired = false;
            debugMessageCache.clear();
            debugMessageCache.add("");
            debugMessageCache.add(TextFormatting.BLUE + "[JustEnoughPatches] Ver: " + StellarCore.VERSION);
            debugMessageCache.addAll(PacketProfiler.getProfilerMessages(debugPacketProfilerMessageLimit));
            debugMessageCache.addAll(TEUpdatePacketProfiler.getProfilerMessages(debugTEPacketProfilerMessageLimit));
        }

        List<String> right = event.getRight();
        right.addAll(debugMessageCache);
    }
}
