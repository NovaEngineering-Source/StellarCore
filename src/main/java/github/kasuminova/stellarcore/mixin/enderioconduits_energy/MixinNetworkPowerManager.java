package github.kasuminova.stellarcore.mixin.enderioconduits_energy;

import crazypants.enderio.base.power.IPowerInterface;
import crazypants.enderio.conduits.conduit.power.IPowerConduit;
import crazypants.enderio.conduits.conduit.power.NetworkPowerManager;
import crazypants.enderio.conduits.conduit.power.PowerConduitNetwork;
import crazypants.enderio.conduits.conduit.power.PowerTracker;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.mixin.util.ICapBankSupply;
import github.kasuminova.stellarcore.mixin.util.IStellarNetworkPowerManager;
import github.kasuminova.stellarcore.mixin.util.ReceptorPowerInterface;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

@Mixin(value = NetworkPowerManager.class, remap = false)
public abstract class MixinNetworkPowerManager implements IStellarNetworkPowerManager {

    // Shadows...

    @Shadow
    @Final
    @Nonnull
    private PowerTracker networkPowerTracker;

    @Shadow
    private long energyStored;

    @Shadow
    @Final
    @Nonnull
    private List<PowerConduitNetwork.ReceptorEntry> receptors;

    @Shadow
    @Final
    @Nonnull
    private List<PowerConduitNetwork.ReceptorEntry> storageReceptors;

    @Shadow
    protected abstract void trackerStartTick();

    @Shadow
    protected abstract void trackerEndTick();

    @Shadow
    protected abstract void trackerSend(@Nonnull final IPowerConduit con, final int sent, final boolean fromBank);

    @Shadow
    protected abstract void checkReceptors();

    @Shadow
    protected abstract void updateNetworkStorage();

    @Shadow
    protected abstract void distributeStorageToConduits();

    // Unique fields...

    @Unique
    private final List<ReceptorPowerInterface> stellar_core$collectedPowerInterface = new ObjectArrayList<>();

    @Unique
    private ForkJoinTask<?> stellar_core$parallelTask = null;

    @Unique
    private volatile boolean stellar_core$shouldFinalApply = false;

    // Reflections...

    @Unique
    private static volatile MethodHandle stellar_core$getCapSupply = null;

    /**
     * @author Kasumi_Nova
     * @reason Rewrite to parallel exec.
     */
    @Inject(method = "doApplyRecievedPower", at = @At("HEAD"), cancellable = true)
    public void doApplyRecievedPower(final Profiler theProfiler, final CallbackInfo ci) {
        if (!StellarCoreConfig.PERFORMANCE.enderIOConduits.networkPowerManager) {
            return;
        }
        ci.cancel();

        trackerStartTick();
        checkReceptors();

        stellar_core$parallelTask = ForkJoinPool.commonPool().submit(() -> {
            // Update our energy stored based on what's in our conduits
            updateNetworkStorage();
            networkPowerTracker.tickStart(energyStored);

            ICapBankSupply capSupply = stellar_core$getCapSupply();
            capSupply.invokeInit();

            long available = energyStored + capSupply.getCanExtract();

            if (available <= 0 || (receptors.isEmpty() && storageReceptors.isEmpty())) {
                trackerEndTick();
                networkPowerTracker.tickEnd(energyStored);
                stellar_core$shouldFinalApply = false;
                return;
            }

            stellar_core$collectedPowerInterface.clear();
            receptors.forEach(receptor -> {
                AccessorReceptorEntry accessorReceptorEntry = (AccessorReceptorEntry) receptor;
                IPowerInterface pp = accessorReceptorEntry.invokeGetPowerInterface();
                if (pp != null) {
                    stellar_core$collectedPowerInterface.add(new ReceptorPowerInterface(pp, accessorReceptorEntry));
                }
            });
            stellar_core$shouldFinalApply = true;
        });
    }

    @Unique
    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void finalApplyReceivedPower() {
        if (!StellarCoreConfig.PERFORMANCE.enderIOConduits.networkPowerManager) {
            return;
        }

        if (stellar_core$parallelTask != null && !stellar_core$parallelTask.isDone()) {
            stellar_core$parallelTask.join();
        }
        stellar_core$parallelTask = null;
        if (!stellar_core$shouldFinalApply) {
            return;
        }

        // 我们已经在 doApplyRecievedPower 执行过一次 updateNetworkStorage 了，为什么他妈还要在这里跑一次？
        updateNetworkStorage();
        ICapBankSupply capSupply = stellar_core$getCapSupply();

        long available = energyStored + capSupply.getCanExtract();
        long wasAvailable = available;

        for (final ReceptorPowerInterface rpp : stellar_core$collectedPowerInterface) {
            AccessorReceptorEntry r = rpp.receptor();
            IPowerInterface pp = rpp.pp();

            int canOffer = (int) Math.min(r.getEmmiter().getMaxEnergyExtracted(r.getDirection()), available);
            int used = Math.max(0, pp.receiveEnergy(canOffer, false));
            trackerSend(r.getEmmiter(), used, false);
            available -= used;

            if (available <= 0) {
                break;
            }
        }

        long used = wasAvailable - available;
        // use all the capacator storage first
        energyStored -= used;

        if (!capSupply.getCapBanks().isEmpty()) {
            long capBankChange = 0;
            if (energyStored < 0) {
                // not enough so get the rest from the capacitor bank
                capBankChange = energyStored;
                energyStored = 0;
            } else if (energyStored > 0) {
                // push as much as we can back to the cap banks
                capBankChange = Math.min(energyStored, capSupply.getCanFill());
                energyStored -= capBankChange;
            }

            if (capBankChange < 0) {
                capSupply.invokeRemove(Math.abs(capBankChange));
            } else if (capBankChange > 0) {
                energyStored += capSupply.invokeAdd(capBankChange);
            }

            capSupply.invokeBalance();
        }

        distributeStorageToConduits();

        trackerEndTick();

        networkPowerTracker.tickEnd(energyStored);
    }

    @SuppressWarnings("DataFlowIssue")
    @Unique
    private ICapBankSupply stellar_core$getCapSupply() {
        if (stellar_core$getCapSupply == null) {
            synchronized (NetworkPowerManager.class) {
                if (stellar_core$getCapSupply == null) {
                    try {
                        stellar_core$getCapSupply = MethodHandles.lookup().unreflectGetter(NetworkPowerManager.class.getDeclaredField("capSupply"));
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        try {
            return (ICapBankSupply) stellar_core$getCapSupply.invoke((NetworkPowerManager) (Object) this);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
