package github.kasuminova.stellarcore.common.entity;

import github.kasuminova.stellarcore.client.util.ClassSet;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;

public class EntityForceUpdateManager extends ClassSet {

    public static final EntityForceUpdateManager INSTANCE = new EntityForceUpdateManager();

    @SubscribeEvent
    public void onEntityCanUpdate(final EntityEvent.CanUpdate event) {
        if (isInSet(event.getEntity().getClass())) {
            event.setCanUpdate(true);
        }
    }

    @Override
    public void reload() {
        classSet.clear();
        Arrays.stream(StellarCoreConfig.FEATURES.vanilla.forceUpdateEntityClasses)
                .forEach(className -> findClass(className).ifPresent(this::add));
    }

}
