package github.kasuminova.stellarcore.mixin.ebwizardry_early;

import github.kasuminova.stellarcore.common.integration.ebwizardry.DispenserCastingCompat;
import github.kasuminova.stellarcore.common.mod.Mods;
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
        if (Mods.EBWIZARDRY.loaded()) {
            stellar_core$handleEBWizardryUpdate();
        }
    }

    @Unique
    @Optional.Method(modid = "ebwizardry")
    private void stellar_core$handleEBWizardryUpdate() {
        DispenserCastingCompat.handleEBWizardryUpdate((TileEntityDispenser) (Object) this);
    }

}
