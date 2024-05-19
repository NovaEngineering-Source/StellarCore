package github.kasuminova.stellarcore.common.integration.ftbquests;

import com.feed_the_beast.ftbquests.util.FTBQuestsInventoryListener;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;

public class FTBQInvListener {
    
    public static final FTBQInvListener INSTANCE = new FTBQInvListener();
    
    private final ReferenceSet<EntityPlayerMP> requiredToDetect = new ReferenceOpenHashSet<>();
    
    private FTBQInvListener() {
    }

    @Optional.Method(modid = "ftbquests")
    public void detect() {
        requiredToDetect.forEach(playerMP -> FTBQuestsInventoryListener.detect(playerMP, ItemStack.EMPTY, 0));
        requiredToDetect.clear();
    }

    @Optional.Method(modid = "ftbquests")
    public void addRequiredToDetect(EntityPlayerMP player) {
        requiredToDetect.add(player);
    }

}
