package github.kasuminova.stellarcore.client;


import github.kasuminova.stellarcore.client.handler.ClientEventHandler;
import github.kasuminova.stellarcore.client.hitokoto.HitokotoAPI;
import github.kasuminova.stellarcore.client.util.TitleUtils;
import github.kasuminova.stellarcore.common.CommonProxy;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.concurrent.CompletableFuture;

public class ClientProxy extends CommonProxy {

    @Override
    public void construction() {
        super.construction();

        TitleUtils.setRandomTitle("*Construction*");
    }

    @Override
    public void preInit() {
        super.preInit();
        MinecraftForge.EVENT_BUS.register(ClientEventHandler.INSTANCE);

        TitleUtils.setRandomTitle("*PreInit*");
    }

    @Override
    public void init() {
        super.init();

        TitleUtils.setRandomTitle("*Init*");
    }

    @Override
    public void postInit() {
        super.postInit();

        TitleUtils.setRandomTitle("*PostInit*");
    }

    @Override
    public void loadComplete() {
        super.loadComplete();

        TitleUtils.setRandomTitle();
    }

}
