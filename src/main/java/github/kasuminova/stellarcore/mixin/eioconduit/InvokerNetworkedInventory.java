package github.kasuminova.stellarcore.mixin.eioconduit;

import crazypants.enderio.conduits.conduit.item.NetworkedInventory;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NetworkedInventory.class)
public interface InvokerNetworkedInventory {

    @Invoker(remap = false)
    boolean callCanInsert();

    @Invoker(remap = false)
    IItemHandler callGetInventory();

}
