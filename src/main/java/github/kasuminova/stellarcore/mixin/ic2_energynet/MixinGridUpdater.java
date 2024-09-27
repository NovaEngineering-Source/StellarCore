package github.kasuminova.stellarcore.mixin.ic2_energynet;

import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.util.IC2EnergySyncCalcTask;
import github.kasuminova.stellarcore.mixin.util.IStellarEnergyCalculatorLeg;
import ic2.core.energy.grid.EnergyNetGlobal;
import ic2.core.energy.grid.EnergyNetLocal;
import ic2.core.energy.grid.Grid;
import ic2.core.energy.grid.IEnergyCalculator;
import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;
import io.netty.util.internal.shaded.org.jctools.queues.atomic.MpscLinkedAtomicQueue;
import org.spongepowered.asm.mixin.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

@SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter", "unchecked"})
@Mixin(targets = "ic2.core.energy.grid.GridUpdater", remap = false)
public class MixinGridUpdater {

    @Shadow
    private boolean busy;

    @Shadow
    private boolean isChangeStep;

    @Shadow
    @Final
    private EnergyNetLocal enet;

    @Unique
    private static volatile MethodHandle stellar_core$gridCalcTaskConstructor = null;

    @Unique
    private static volatile MethodHandle stellar_core$gridCalcTaskGridSetter = null;

    @Unique
    private static volatile MethodHandle stellar_core$EnergyNetGlobal$getCalculator = null;

    @Unique
    private static volatile MethodHandle stellar_core$EnergyNetLocal$hasGrids = null;

    @Unique
    private static volatile MethodHandle stellar_core$EnergyNetLocal$getGrids = null;

    @Unique
    private static volatile MethodHandle stellar_core$EnergyNetLocal$shuffleGrids = null;

    @Unique
    private final Queue<IC2EnergySyncCalcTask> stellar_core$syncTaskQueue = stellar_core$createMpscQueue();

    /**
     * @author Kasumi_Nova
     * @reason Parallel Calculation
     */
    @Overwrite
    void startTransferCalc() {
        assert !this.busy;
        this.isChangeStep = false;
        IEnergyCalculator energyCalculator = stellar_core$EnergyNetGlobal$getCalculator();
        if (!stellar_core$EnergyNetLocal$hasGrids(enet) || !energyCalculator.runSyncStep(this.enet)) {
            return;
        }

        this.busy = true;
        Collection<Grid> grids = stellar_core$EnergyNetLocal$getGrids(enet);

        IStellarEnergyCalculatorLeg stellarCalculator = (IStellarEnergyCalculatorLeg) energyCalculator;

        Queue<Grid> calculateQueue = stellar_core$createMpmcQueue(grids.size());
        calculateQueue.addAll(grids);
        int tasks = grids.size();
        ForkJoinPool.commonPool().submit(() -> {
            int concurrency = Math.min(tasks, Math.max(Runtime.getRuntime().availableProcessors(), 2));
            IntStream.range(0, concurrency).forEach(i -> ForkJoinPool.commonPool().submit(() -> {
                Grid grid;
                while ((grid = calculateQueue.poll()) != null) {
                    stellar_core$syncTaskQueue.offer(stellarCalculator.doParallelCalc(grid));
                }
            }));
        });

        stellar_core$executeSyncTasks(tasks, stellarCalculator, calculateQueue);

        if (!stellar_core$syncTaskQueue.isEmpty()) {
            StellarLog.LOG.warn("[StellarCore-IC2GridUpdater] Unable to complete all tasks, {} tasks left.", stellar_core$syncTaskQueue.size());
            stellar_core$syncTaskQueue.forEach(stellarCalculator::doSyncCalc);
            stellar_core$syncTaskQueue.clear();
        }

        if (grids.size() > 1) {
            stellar_core$EnergyNetLocal$shuffleGrids(enet);
        }

        this.busy = false;
    }

    @Unique
    private void stellar_core$executeSyncTasks(final int totalTasks, final IStellarEnergyCalculatorLeg stellarCalculator, final Queue<Grid> calculateQueue) {
        int completedTask = 0;
        IC2EnergySyncCalcTask task;
        boolean syncBusy;
        while (completedTask < totalTasks) {
            syncBusy = true;

            while ((task = stellar_core$syncTaskQueue.poll()) != null) {
                stellarCalculator.doSyncCalc(task);
                completedTask++;
                syncBusy = false;
            }

            if (stellar_core$helpComplete(stellarCalculator, calculateQueue)) {
                completedTask++;
                syncBusy = false;
            }

            if (syncBusy) {
                stellar_core$awaitCompletion();
            }
        }
    }

    @Unique
    private static boolean stellar_core$helpComplete(final IStellarEnergyCalculatorLeg stellarCalculator, final Queue<Grid> calculateQueue) {
        final Grid grid = calculateQueue.poll();
        if (grid != null) {
            stellarCalculator.doSyncCalc(stellarCalculator.doParallelCalc(grid));
            return true;
        }
        return false;
    }

    @Unique
    private static void stellar_core$awaitCompletion() {
        try {
            Thread.sleep(0);
        } catch (InterruptedException ignored) {
        }
    }

    @Unique
    private static Object stellar_core$newGridCalcTask(final Object gridUpdater) {
        if (stellar_core$gridCalcTaskConstructor == null) {
            synchronized (gridUpdater) {
                if (stellar_core$gridCalcTaskConstructor == null) {
                    try {
                        Constructor<?> constructor = Class.forName("ic2.core.energy.grid.GridUpdater$GridCalcTask").getDeclaredConstructor(Class.forName("ic2.core.energy.grid.GridUpdater"));
                        constructor.setAccessible(true);
                        stellar_core$gridCalcTaskConstructor = MethodHandles.lookup().unreflectConstructor(constructor);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        try {
            return stellar_core$gridCalcTaskConstructor.invoke(gridUpdater);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private static void stellar_core$setGridCalcTaskGrid(final Object gridCalcTask, final Grid grid) {
        if (stellar_core$gridCalcTaskGridSetter == null) {
            synchronized (gridCalcTask) {
                if (stellar_core$gridCalcTaskGridSetter == null) {
                    try {
                        Field gridField = Class.forName("ic2.core.energy.grid.GridUpdater$GridCalcTask").getDeclaredField("grid");
//                        gridField.setAccessible(true);
                        stellar_core$gridCalcTaskGridSetter = MethodHandles.lookup().unreflectSetter(gridField);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        try {
            stellar_core$gridCalcTaskGridSetter.invoke(gridCalcTask, grid);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private static IEnergyCalculator stellar_core$EnergyNetGlobal$getCalculator() {
        if (stellar_core$EnergyNetGlobal$getCalculator == null) {
            synchronized (EnergyNetGlobal.class) {
                if (stellar_core$EnergyNetGlobal$getCalculator == null) {
                    try {
                        stellar_core$EnergyNetGlobal$getCalculator = MethodHandles.lookup().unreflect(Class.forName("ic2.core.energy.grid.EnergyNetGlobal").getDeclaredMethod("getCalculator"));
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        try {
            return (IEnergyCalculator) stellar_core$EnergyNetGlobal$getCalculator.invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private static boolean stellar_core$EnergyNetLocal$hasGrids(final EnergyNetLocal enet) {
        if (stellar_core$EnergyNetLocal$hasGrids == null) {
            synchronized (EnergyNetLocal.class) {
                if (stellar_core$EnergyNetLocal$hasGrids == null) {
                    try {
                        stellar_core$EnergyNetLocal$hasGrids = MethodHandles.lookup().unreflect(Class.forName("ic2.core.energy.grid.EnergyNetLocal").getDeclaredMethod("hasGrids"));
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        try {
            return (boolean) stellar_core$EnergyNetLocal$hasGrids.invoke(enet);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private static Collection<Grid> stellar_core$EnergyNetLocal$getGrids(final EnergyNetLocal enet) {
        if (stellar_core$EnergyNetLocal$getGrids == null) {
            synchronized (EnergyNetLocal.class) {
                if (stellar_core$EnergyNetLocal$getGrids == null) {
                    try {
                        stellar_core$EnergyNetLocal$getGrids = MethodHandles.lookup().unreflect(Class.forName("ic2.core.energy.grid.EnergyNetLocal").getDeclaredMethod("getGrids"));
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        try {
            return (Collection<Grid>) stellar_core$EnergyNetLocal$getGrids.invoke(enet);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private static void stellar_core$EnergyNetLocal$shuffleGrids(final EnergyNetLocal enet) {
        if (stellar_core$EnergyNetLocal$shuffleGrids == null) {
            synchronized (EnergyNetLocal.class) {
                if (stellar_core$EnergyNetLocal$shuffleGrids == null) {
                    try {
                        stellar_core$EnergyNetLocal$shuffleGrids = MethodHandles.lookup().unreflect(Class.forName("ic2.core.energy.grid.EnergyNetLocal").getDeclaredMethod("shuffleGrids"));
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        try {
            stellar_core$EnergyNetLocal$shuffleGrids.invoke(enet);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    private static <E> Queue<E> stellar_core$createMpscQueue() {
        try {
            // May be incompatible with cleanroom.
            return new MpscLinkedAtomicQueue<>();
        } catch (Throwable e) {
            return new ConcurrentLinkedQueue<>();
        }
    }

    @Unique
    private static <E> Queue<E> stellar_core$createMpmcQueue(final int capacity) {
        try {
            // May be incompatible with cleanroom.
            return new MpmcArrayQueue<>(capacity);
        } catch (Throwable e) {
            return new ConcurrentLinkedQueue<>();
        }
    }

}
