package github.kasuminova.stellarcore.mixin.ebwizardry;

import electroblob.wizardry.data.DispenserCastingData;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.ITickable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TileEntityDispenser.class)
public abstract class MixinTileEntityDispenser extends TileEntityLockableLoot implements ITickable {

    @Unique
    @Override
    @SuppressWarnings({"DataFlowIssue", "AddedMixinMembersNamePattern"})
    public void update() {
        if (world.isRemote) {
            return;
        }
        DispenserCastingData data = DispenserCastingData.get((TileEntityDispenser) (Object) this);
        if (data != null) {
            data.update();
        }
    }

}
