package github.kasuminova.stellarcore.common.integration.ebwizardry;

import electroblob.wizardry.data.DispenserCastingData;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraftforge.fml.common.Optional;

public class DispenserCastingCompat {

    @Optional.Method(modid = "ebwizardry")
    public static void handleEBWizardryUpdate(final TileEntityDispenser dispenser) {
        DispenserCastingData data = DispenserCastingData.get(dispenser);
        if (data != null) {
            data.update();
        }
    }

}
