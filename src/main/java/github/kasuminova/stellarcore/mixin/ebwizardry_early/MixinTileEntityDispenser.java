package github.kasuminova.stellarcore.mixin.ebwizardry_early;

import electroblob.wizardry.data.DispenserCastingData;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.common.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TileEntityDispenser.class)
public abstract class MixinTileEntityDispenser extends TileEntityLockableLoot implements ITickable {

    @Unique
    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void update() {
        if (world.isRemote) {
            return;
        }
        stellar_core$handleEBWizardryUpdate();
    }

    @Unique
    @Optional.Method(modid = "ebwizardry")
    private void stellar_core$handleEBWizardryUpdate() {
        DispenserCastingData data = DispenserCastingData.get((TileEntityDispenser) (Object) this);
        if (data != null) {
            data.update();
        }
    }

}
