package github.kasuminova.stellarcore.common.integration.ftblib;

import com.feed_the_beast.ftblib.lib.util.InvUtils;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Optional;

public class FTBLibInvUtilsQueue {

    public static final FTBLibInvUtilsQueue INSTANCE = new FTBLibInvUtilsQueue();

    private final ReferenceSet<EntityPlayer> requiredToUpdate = new ReferenceOpenHashSet<>();

    private FTBLibInvUtilsQueue() {
    }

    @Optional.Method(modid = "ftblib")
    public void updateAll() {
        requiredToUpdate.forEach(player -> InvUtils.forceUpdate(player.inventoryContainer));
        requiredToUpdate.clear();
    }

    @Optional.Method(modid = "ftblib")
    public void enqueuePlayer(final EntityPlayer player) {
        requiredToUpdate.add(player);
    }

}
