package github.kasuminova.stellarcore.mixin.ebwizardry;

import electroblob.wizardry.data.DispenserCastingData;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.mod.Mods;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(value = DispenserCastingData.class, remap = false)
public abstract class MixinDispenserCastingData {

    @Inject(method = "onWorldTickEvent", at = @At("HEAD"), cancellable = true)
    private static void injectOnWorldTickEvent(final TickEvent.WorldTickEvent event, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.ebWizardry.dispenserCastingData) {
            return;
        }
        if (!Mods.TICK_CENTRAL.loaded()) {
            ci.cancel();
            return;
        }

        if (event.phase == TickEvent.Phase.START || event.world.loadedTileEntityList.size() <= 5_000) {
            return;
        }
        ci.cancel();

        // Use parallel stream to find Capability to improve performance.
        try {
            stellar_core$executeParallel(event.world.loadedTileEntityList);
        } catch (ConcurrentModificationException | NullPointerException e) {
            // CME?
            stellar_core$executeParallel(new ArrayList<>(event.world.loadedTileEntityList));
        }
    }

    @Unique
    private static void stellar_core$executeParallel(final List<TileEntity> tileEntityList) {
        tileEntityList.parallelStream()
                .filter(TileEntityDispenser.class::isInstance)
                .map(TileEntityDispenser.class::cast)
                .map(DispenserCastingData::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedList::new))
                .forEach(DispenserCastingData::update);
    }

}
