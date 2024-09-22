package github.kasuminova.stellarcore.mixin.enderioconduits_energy;

import crazypants.enderio.base.power.IPowerStorage;
import github.kasuminova.stellarcore.mixin.util.ICapBankSupply;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;
import java.util.Set;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(targets = "crazypants.enderio.conduits.conduit.power.NetworkPowerManager$CapBankSupply", remap = false)
public abstract class MixinCapBankSupply implements ICapBankSupply {

    @Shadow
    int canFill;

    @Shadow
    int canExtract;

    @Shadow
    @Final
    @Nonnull
    Set<IPowerStorage> capBanks;

    @Shadow
    abstract void init();

    @Shadow
    abstract void balance();

    @Shadow
    abstract void remove(final long amount);

    @Shadow
    abstract long add(final long amount);

    @Override
    public int getCanExtract() {
        return canExtract;
    }

    @Override
    public int getCanFill() {
        return canFill;
    }

    @Nonnull
    @Override
    public Set<IPowerStorage> getCapBanks() {
        return capBanks;
    }

    @Override
    public void invokeInit() {
        init();
    }

    @Override
    public void invokeBalance() {
        balance();
    }

    @Override
    public void invokeRemove(final long remove) {
        remove(remove);
    }

    @Override
    public long invokeAdd(final long add) {
        return add(add);
    }

}
