package github.kasuminova.stellarcore.mixin.ic2;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.BlockPos2IntMap;
import github.kasuminova.stellarcore.common.util.BlockPos2ValueMap;
import ic2.api.energy.tile.IEnergyTile;
import ic2.core.energy.grid.EnergyNetLocal;
import ic2.core.energy.grid.Tile;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Queue;

@Mixin(value = EnergyNetLocal.class, remap = false)
public abstract class MixinEnergyNetLocal {

    @Final
    @Shadow
    @Mutable
    Map<BlockPos, Tile> registeredTiles;

    @Unique
    private final BlockPos2IntMap stellar_core$gridChangeCounter = new BlockPos2IntMap();

    @Unique
    private static final Object STELLAR_CORE$QUEUE_DELAY_CHANGE = stellar_core$getQueueDelayChange();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.industrialCraft2.energyNetLocal) {
            return;
        }
        this.registeredTiles = new BlockPos2ValueMap<>();
    }

    @Inject(method = "getIoTile", at = @At(value = "INVOKE", target = "Ljava/util/Queue;iterator()Ljava/util/Iterator;"), cancellable = true)
    private void injectGetIOTileBeforeScanQueue(final BlockPos pos, final CallbackInfoReturnable<IEnergyTile> cir) {
        if (!StellarCoreConfig.PERFORMANCE.industrialCraft2.getIoAndSubTile) {
            return;
        }
        if (stellar_core$gridChangeCounter.getInt(pos) <= 0) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "getSubTile", at = @At(value = "INVOKE", target = "Ljava/util/Queue;iterator()Ljava/util/Iterator;"), cancellable = true)
    private void injectGetSubTileBeforeScan(final BlockPos pos, final CallbackInfoReturnable<IEnergyTile> cir) {
        if (!StellarCoreConfig.PERFORMANCE.industrialCraft2.getIoAndSubTile) {
            return;
        }
        if (stellar_core$gridChangeCounter.getInt(pos) <= 0) {
            cir.setReturnValue(null);
        }
    }

    @Redirect(method = {"addTile", "removeTile"}, at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
    private boolean redirectQueueAdd(final Queue<Object> instance, final Object e) {
        if (e != STELLAR_CORE$QUEUE_DELAY_CHANGE && e instanceof AccessorGridChange gridChange) {
            stellar_core$gridChangeCounter.addTo(gridChange.getPos(), 1);
        }
        return instance.add(e);
    }

    @Redirect(method = "removeTile", at = @At(value = "INVOKE", target = "Ljava/util/Queue;remove(Ljava/lang/Object;)Z"))
    private boolean redirectQueueRemove(final Queue<Object> instance, final Object e) {
        if (e != STELLAR_CORE$QUEUE_DELAY_CHANGE && e instanceof AccessorGridChange gridChange) {
            stellar_core$gridChangeCounter.addTo(gridChange.getPos(), -1);
        }
        return instance.remove(e);
    }

    @Redirect(method = "onTickEnd", at = @At(value = "INVOKE", target = "Ljava/util/Queue;poll()Ljava/lang/Object;"))
    private Object redirectQueuePoll(final Queue<Object> instance) {
        Object polled = instance.poll();
        if (polled != STELLAR_CORE$QUEUE_DELAY_CHANGE && polled instanceof AccessorGridChange gridChange) {
            stellar_core$gridChangeCounter.addTo(gridChange.getPos(), -1);
        }
        return polled;
    }

    @Unique
    private static Object stellar_core$getQueueDelayChange() {
        try {
            Field queueDelayChange = EnergyNetLocal.class.getDeclaredField("QUEUE_DELAY_CHANGE");
            queueDelayChange.setAccessible(true);
            return queueDelayChange.get(null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
