package github.kasuminova.stellarcore.common.handler;

import github.kasuminova.stellarcore.common.integration.ftblib.FTBLibInvUtilsQueue;
import github.kasuminova.stellarcore.common.integration.ftbquests.FTBQInvListener;
import github.kasuminova.stellarcore.common.mod.Mods;
import github.kasuminova.stellarcore.common.util.StellarLog;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class StellarCoreTickHandler {

    private static int tickExisted = 0;

    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.side.isClient()) {
            return;
        }
        tickExisted++;

        if (Mods.FTBLIB.loaded()) {
            updatePlayerInv();
        }
        if (Mods.FTBQ.loaded()) {
            detectFTBQTasks();
        }

        updateServerThreadPriority();
    }

    private static void updateServerThreadPriority() {
        final Thread current = Thread.currentThread();
        if (FMLCommonHandler.instance().getMinecraftServerInstance().isCallingFromMinecraftThread()) {
            if (current.getPriority() != Thread.MAX_PRIORITY) {
                current.setPriority(Thread.MAX_PRIORITY);
                StellarLog.LOG.info("[StellarCore] Set server thread priority to MAX.");
            }
        }
    }

    @Optional.Method(modid = "ftblib")
    private static void updatePlayerInv() {
        FTBLibInvUtilsQueue.INSTANCE.updateAll();
    }

    @Optional.Method(modid = "ftbquests")
    private static void detectFTBQTasks() {
        if (tickExisted % 60 == 0) {
            FTBQInvListener.INSTANCE.detect();
        }
    }

}
