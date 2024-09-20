package github.kasuminova.stellarcore.mixin.ebwizardry;

import electroblob.wizardry.data.DispenserCastingData;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

@Mixin(value = DispenserCastingData.class, remap = false)
public abstract class MixinDispenserCastingData {

    @SuppressWarnings("SimplifyStreamApiCallChains")
    @Inject(method = "onWorldTickEvent", at = @At("HEAD"), cancellable = true)
    private static void injectOnWorldTickEvent(final TickEvent.WorldTickEvent event, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.ebWizardry.dispenserCastingData) {
            return;
        }
        if (event.phase == TickEvent.Phase.START || event.world.loadedTileEntityList.size() <= 5_000) {
            return;
        }
        ci.cancel();

        // Use parallel stream to find Capability to improve performance.
        new ArrayList<>(event.world.loadedTileEntityList)
                .parallelStream()
                .filter(TileEntityDispenser.class::isInstance)
                .map(TileEntityDispenser.class::cast)
                .map(DispenserCastingData::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                .forEach(DispenserCastingData::update);
    }

}
