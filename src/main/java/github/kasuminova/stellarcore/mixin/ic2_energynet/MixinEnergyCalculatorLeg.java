package github.kasuminova.stellarcore.mixin.ic2_energynet;

import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.config.category.Performance.IndustrialCraft2.EnergyCalculatorLegParallelMode;
import github.kasuminova.stellarcore.common.mod.Mods;
import github.kasuminova.stellarcore.common.util.StellarEnvironment;
import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.AccessorGridData;
import github.kasuminova.stellarcore.mixin.util.IC2EnergySyncCalcTask;
import github.kasuminova.stellarcore.mixin.util.IStellarEnergyCalculatorLeg;
import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IMultiEnergySource;
import ic2.core.IC2;
import ic2.core.energy.grid.*;
import ic2.core.energy.leg.EnergyCalculatorLeg;
import ic2.core.util.LogCategory;
import ic2.core.util.Util;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = EnergyCalculatorLeg.class, remap = false)
public abstract class MixinEnergyCalculatorLeg implements IStellarEnergyCalculatorLeg {

    @Unique
    private static volatile MethodHandle stellar_core$distribute = null;

    @Unique
    private static volatile MethodHandle stellar_core$getData = null;

    @Unique
    private static EnergyCalculatorLegParallelMode stellar_core$getEffectiveParallelMode() {
        final EnergyCalculatorLegParallelMode configured = StellarCoreConfig.PERFORMANCE.industrialCraft2.energyCalculatorLegParallelMode;
        if (configured == null || configured == EnergyCalculatorLegParallelMode.AUTO) {
            // Galacticraft energy network is not thread-safe (see Issue #36).
            if (Mods.GC.loaded()) {
                return EnergyCalculatorLegParallelMode.SEMI_ASYNC;
            }
            return EnergyCalculatorLegParallelMode.FULL_ASYNC;
        }
        return configured;
    }

    @Shadow
    @SuppressWarnings("rawtypes")
    private static void applyCableEffects(final Collection eventPaths, final World world) {
    }

    /**
     * @author Kasumi_Nova
     * @reason Parallel calculation
     */
    @Inject(method = "runSyncStep(Lic2/core/energy/grid/EnergyNetLocal;)Z", at = @At("HEAD"), cancellable = true)
    public void runSyncStep(final EnergyNetLocal enet, final CallbackInfoReturnable<Boolean> cir) {
        if (!StellarCoreConfig.PERFORMANCE.industrialCraft2.energyCalculatorLeg || !StellarEnvironment.shouldParallel()) {
            return;
        }

        final boolean fullAsync = stellar_core$getEffectiveParallelMode() == EnergyCalculatorLegParallelMode.FULL_ASYNC;

        final AtomicBoolean foundAny = new AtomicBoolean(false);
        try {
            (fullAsync ? enet.getSources().parallelStream() : enet.getSources().stream()).forEach(tile -> {
                IEnergySource source = (IEnergySource) tile.getMainTile();
                int packets = 1;
                double amount;
                if (tile.isDisabled() || !((amount = source.getOfferedEnergy()) > 0.0D) ||
                    (source instanceof IMultiEnergySource multiSource && (multiSource).sendMultipleEnergyPackets() && (packets = multiSource.getMultipleEnergyPacketAmount()) <= 0)) {
                    tile.setSourceData(0.0D, 0);
                    return;
                }

                int tier = source.getSourceTier();
                if (tier < 0) {
                    if (EnergyNetSettings.logGridCalculationIssues) {
                        IC2.log.warn(LogCategory.EnergyNet, "Tile %s reported an invalid tier (%d).", Util.toString(source, enet.getWorld(), EnergyNet.instance.getPos(source)), tier);
                    }
                    tile.setSourceData(0.0D, 0);
                    return;
                }
                foundAny.set(true);
                double power = EnergyNet.instance.getPowerFromTier(tier);
                amount = Math.min(amount, power * packets);
                tile.setSourceData(amount, packets);
            });
        } catch (Throwable e) {
            StellarLog.LOG.warn("[StellarCore-MixinEnergyCalculatorLeg] Failed to execute energy grids data!", e);
        }

        cir.setReturnValue(foundAny.get());
    }

    @Unique
    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public IC2EnergySyncCalcTask doParallelCalc(final Grid grid) {
        AccessorGridData gridData = stellar_core$getData(grid);
        if (!gridData.isActive()) {
            return IC2EnergySyncCalcTask.EMPTY;
        }

        final boolean fullAsync = stellar_core$getEffectiveParallelMode() == EnergyCalculatorLegParallelMode.FULL_ASYNC;

        List<Node> activeSources = gridData.getActiveSources();
        Map<Node, MutableDouble> activeSinks = gridData.getActiveSinks();

        activeSources.clear();
        activeSinks.clear();

        int calcId = gridData.incrementCurrentCalcId();

        for (Node node : grid.getNodes()) {
            Tile tile = node.getTile();
            if (tile.isDisabled()) {
                continue;
            }
            if (node.getType() == NodeType.Source && gridData.getEnergySourceToEnergyPathMap().containsKey(node) && tile.getAmount() > 0.0D) {
                activeSources.add(node);
                continue;
            }
            if (node.getType() == NodeType.Sink) {
                if (fullAsync) {
                    double amount;
                    if ((amount = ((IEnergySink) tile.getMainTile()).getDemandedEnergy()) > 0.0D) {
                        activeSinks.put(node, new MutableDouble(amount));
                    }
                } else {
                    // Defer demanded energy calculation to the sync phase to avoid calling external mod code off-thread.
                    activeSinks.put(node, new MutableDouble(Double.NaN));
                }
            }
        }

        if (activeSources.isEmpty() || activeSinks.isEmpty()) {
            return IC2EnergySyncCalcTask.EMPTY;
        }

        World world = grid.getEnergyNet().getWorld();
        return new IC2EnergySyncCalcTask(world, calcId, grid, gridData, activeSources, activeSinks);
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void doSyncCalc(final IC2EnergySyncCalcTask task) {
        if (task == IC2EnergySyncCalcTask.EMPTY) {
            return;
        }

        World world = task.world();
        List<Node> activeSources = task.activeSources();
        Map<Node, MutableDouble> activeSinks = task.activeSinks();
        AccessorGridData gridData = task.gridData();
        int calcID = task.calcID();
        Grid grid = task.grid();

        if (stellar_core$needsSyncDemandCalculation(activeSinks)) {
            stellar_core$syncCalculateDemands(activeSinks);
            if (activeSinks.isEmpty()) {
                return;
            }
        }

        Random rand = world.rand;
        boolean shufflePaths = ((world.getTotalWorldTime() & 0x3L) != 0L);

        int sourcesOffset;
        if (activeSources.size() > 1) {
            sourcesOffset = rand.nextInt(activeSources.size());
        } else {
            sourcesOffset = 0;
        }

        int i;
        for (i = sourcesOffset; i < activeSources.size() && !activeSinks.isEmpty(); i++) {
            stellar_core$distribute(activeSources.get(i), gridData, shufflePaths, calcID, rand);
        }
        for (i = 0; i < sourcesOffset && !activeSinks.isEmpty(); i++) {
            stellar_core$distribute(activeSources.get(i), gridData, shufflePaths, calcID, rand);
        }

        Set<Object> eventPaths = gridData.getEventPaths();
        if (!eventPaths.isEmpty()) {
            applyCableEffects(eventPaths, grid.getEnergyNet().getWorld());
            eventPaths.clear();
        }
    }

    @Unique
    private static boolean stellar_core$needsSyncDemandCalculation(final Map<Node, MutableDouble> activeSinks) {
        if (stellar_core$getEffectiveParallelMode() != EnergyCalculatorLegParallelMode.SEMI_ASYNC) {
            return false;
        }
        // In SEMI_ASYNC we always defer sink demanded-energy calculation to the sync phase.
        return true;
    }

    @Unique
    private static void stellar_core$syncCalculateDemands(final Map<Node, MutableDouble> activeSinks) {
        final Iterator<Map.Entry<Node, MutableDouble>> iterator = activeSinks.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Node, MutableDouble> entry = iterator.next();
            final Node node = entry.getKey();
            final Tile tile = node.getTile();
            if (tile.isDisabled()) {
                iterator.remove();
                continue;
            }

            final double amount;
            try {
                amount = ((IEnergySink) tile.getMainTile()).getDemandedEnergy();
            } catch (Throwable e) {
                // Avoid crashing the server tick when a mod has unexpected behaviour.
                iterator.remove();
                continue;
            }

            if (amount > 0.0D) {
                entry.getValue().setValue(amount);
            } else {
                iterator.remove();
            }
        }
    }

    @Unique
    private static void stellar_core$distribute(final Node srcNode, final AccessorGridData gridData, final boolean shufflePaths, final int calcId, final Random rand) {
        if (stellar_core$distribute == null) {
            synchronized (EnergyCalculatorLeg.class) {
                if (stellar_core$distribute == null) {
                    try {
                        stellar_core$distribute = MethodHandles.lookup().unreflect(EnergyCalculatorLeg.class.getDeclaredMethod(
                                "distribute", Node.class, Class.forName("ic2.core.energy.leg.EnergyCalculatorLeg$GridData"), boolean.class, int.class, Random.class
                        ));
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        try {
            stellar_core$distribute.invoke(srcNode, gridData, shufflePaths, calcId, rand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private static AccessorGridData stellar_core$getData(final Grid grid) {
        if (stellar_core$getData == null) {
            synchronized (EnergyCalculatorLeg.class) {
                if (stellar_core$getData == null) {
                    try {
                        stellar_core$getData = MethodHandles.lookup().unreflect(EnergyCalculatorLeg.class.getDeclaredMethod(
                                "getData", Grid.class
                        ));
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        try {
            return (AccessorGridData) stellar_core$getData.invoke(grid);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
