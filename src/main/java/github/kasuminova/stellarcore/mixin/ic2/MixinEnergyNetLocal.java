package github.kasuminova.stellarcore.mixin.ic2;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.BlockPos2ValueMap;
import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyTile;
import ic2.core.energy.grid.EnergyNetLocal;
import ic2.core.energy.grid.Tile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Mixin(value = EnergyNetLocal.class, remap = false)
public abstract class MixinEnergyNetLocal {

    @Final
    @Shadow
    @Mutable
    Map<BlockPos, Tile> registeredTiles;

//    @Unique
//    private final BlockPos2ValueMap<List<AccessorGridChange>> stellar_core$gridChangesMap = new BlockPos2ValueMap<>();
//
//    @Unique
//    private static final Object STELLAR_CORE$QUEUE_DELAY_CHANGE = stellar_core$getQueueDelayChange();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.industrialCraft2.energyNetLocal) {
            return;
        }
        this.registeredTiles = BlockPos2ValueMap.create();
    }

//    @Inject(method = "getIoTile", at = @At(value = "INVOKE", target = "Ljava/util/Queue;iterator()Ljava/util/Iterator;"), cancellable = true)
//    private void injectGetIOTileBeforeScanQueue(final BlockPos pos, final CallbackInfoReturnable<IEnergyTile> cir) {
//        if (!StellarCoreConfig.PERFORMANCE.industrialCraft2.getIoAndSubTile) {
//            return;
//        }
//
//        List<AccessorGridChange> gridChanges = stellar_core$gridChangesMap.get(pos);
//        if (gridChanges == null) {
//            cir.setReturnValue(null);
//            return;
//        }
//
//        gridChanges.stream()
//                .filter(gridChange -> gridChange.getPos().equals(pos))
//                .findFirst()
//                .ifPresent(gridChange -> cir.setReturnValue(gridChange.getIoTile()));
//    }
//
//    @Inject(method = "getSubTile", at = @At(value = "INVOKE", target = "Ljava/util/Queue;iterator()Ljava/util/Iterator;"), cancellable = true)
//    private void injectGetSubTileBeforeScan(final BlockPos pos, final CallbackInfoReturnable<IEnergyTile> cir) {
//        if (!StellarCoreConfig.PERFORMANCE.industrialCraft2.getIoAndSubTile) {
//            return;
//        }
//
//        List<AccessorGridChange> gridChanges = stellar_core$gridChangesMap.get(pos);
//        if (gridChanges == null) {
//            cir.setReturnValue(null);
//            return;
//        }
//
//        for (AccessorGridChange gridChange : gridChanges) {
//            if (!gridChange.getPos().equals(pos)) {
//                continue;
//            }
//
//            List<IEnergyTile> subTiles = gridChange.getSubTiles();
//            if (subTiles == null) {
//                subTiles = Collections.singletonList(gridChange.getIoTile());
//            }
//
//            for (final IEnergyTile subTile : subTiles) {
//                if (EnergyNet.instance.getPos(subTile).equals(pos)) {
//                    cir.setReturnValue(subTile);
//                    return;
//                }
//            }
//        }
//    }
//
//    @Redirect(method = {"addTile", "removeTile"}, at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
//    private boolean redirectQueueAdd(final Queue<Object> instance, final Object e) {
//        if (e != STELLAR_CORE$QUEUE_DELAY_CHANGE && e instanceof AccessorGridChange gridChange) {
//            stellar_core$gridChangesMap.computeIfAbsent(gridChange.getPos(), (key) -> new ObjectArrayList<>()).add(gridChange);
//        }
//        return instance.add(e);
//    }
//
//    @Redirect(method = "removeTile", at = @At(value = "INVOKE", target = "Ljava/util/Queue;remove(Ljava/lang/Object;)Z"))
//    private boolean redirectQueueRemove(final Queue<Object> instance, final Object e) {
//        boolean removed = instance.remove(e);
//        if (removed && e != STELLAR_CORE$QUEUE_DELAY_CHANGE && e instanceof AccessorGridChange gridChange) {
//            List<AccessorGridChange> gridChanges = stellar_core$gridChangesMap.get(gridChange.getPos());
//            if (gridChanges != null) {
//                gridChanges.remove(gridChange);
//                if (gridChanges.isEmpty()) {
//                    stellar_core$gridChangesMap.remove(gridChange.getPos());
//                }
//            }
//        }
//        return removed;
//    }
//
//    @Redirect(method = "onTickEnd", at = @At(value = "INVOKE", target = "Ljava/util/Queue;poll()Ljava/lang/Object;"))
//    private Object redirectQueuePoll(final Queue<Object> instance) {
//        Object polled = instance.poll();
//        if (polled != STELLAR_CORE$QUEUE_DELAY_CHANGE && polled instanceof AccessorGridChange gridChange) {
//            List<AccessorGridChange> gridChanges = stellar_core$gridChangesMap.get(gridChange.getPos());
//            if (gridChanges != null) {
//                gridChanges.remove(gridChange);
//                if (gridChanges.isEmpty()) {
//                    stellar_core$gridChangesMap.remove(gridChange.getPos());
//                }
//            }
//        }
//        return polled;
//    }
//
//    @Unique
//    private static Object stellar_core$getQueueDelayChange() {
//        try {
//            Field queueDelayChange = EnergyNetLocal.class.getDeclaredField("QUEUE_DELAY_CHANGE");
//            queueDelayChange.setAccessible(true);
//            return queueDelayChange.get(null);
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//    }

}
