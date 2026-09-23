package github.kasuminova.stellarcore.mixin.ebwizardry;

import electroblob.wizardry.client.WizardryClientEventHandler;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.mod.Mods;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(value = WizardryClientEventHandler.class, remap = false)
public class MixinWizardryClientEventHandler {

    @Unique
    private static final ArrayList<TileEntity> stellar_core$emptySnapshot = new ArrayList<>(0);

    @Redirect(method = "onClientTickEvent",
        at = @At(value = "NEW", target = "java/util/ArrayList"), require = 0, remap = false)
    private static ArrayList<TileEntity> stellar_core$skipSnapshot(
        final Collection<TileEntity> c) {
        if (StellarCoreConfig.PERFORMANCE.ebWizardry.dispenserCastingData && !Mods.TICK_CENTRAL.loaded()) {
            return stellar_core$emptySnapshot;
        }
        return new ArrayList<>(c);
    }

}
