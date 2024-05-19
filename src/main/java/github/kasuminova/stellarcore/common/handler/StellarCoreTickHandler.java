package github.kasuminova.stellarcore.common.handler;


import github.kasuminova.stellarcore.common.integration.ftbquests.FTBQInvListener;
import github.kasuminova.stellarcore.common.mod.Mods;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class StellarCoreTickHandler {

    private static int tickExisted = 0;

    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) {
            return;
        }
        tickExisted++;

        if (Mods.FTBQ.loaded()) {
            detectFTBQTasks();
        }
    }

    @Optional.Method(modid = "ftbquests")
    protected static void detectFTBQTasks() {
        if (tickExisted % 60 == 0) {
            FTBQInvListener.INSTANCE.detect();
        }
    }

}
