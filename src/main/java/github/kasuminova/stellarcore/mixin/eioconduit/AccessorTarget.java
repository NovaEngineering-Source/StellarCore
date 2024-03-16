package github.kasuminova.stellarcore.mixin.eioconduit;

import crazypants.enderio.conduits.conduit.item.NetworkedInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "crazypants.enderio.conduits.conduit.item.NetworkedInventory$Target")
public interface AccessorTarget {

    @Accessor(remap = false)
    boolean getStickyInput();

    @Accessor(remap = false)
    NetworkedInventory getInv();

}
