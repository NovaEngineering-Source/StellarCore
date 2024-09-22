package github.kasuminova.stellarcore.mixin.enderioconduits_energy;

import crazypants.enderio.conduits.conduit.AbstractConduitNetwork;
import crazypants.enderio.conduits.conduit.power.IPowerConduit;
import crazypants.enderio.conduits.conduit.power.NetworkPowerManager;
import crazypants.enderio.conduits.conduit.power.PowerConduitNetwork;
import github.kasuminova.stellarcore.mixin.util.IStellarNetworkPowerManager;
import github.kasuminova.stellarcore.mixin.util.IStellarServerTickListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = PowerConduitNetwork.class, remap = false)
public abstract class MixinPowerConduitNetwork extends AbstractConduitNetwork<IPowerConduit, IPowerConduit> implements IStellarServerTickListener {

    @Shadow
    NetworkPowerManager powerManager;

    @SuppressWarnings("DataFlowIssue")
    protected MixinPowerConduitNetwork() {
        super(null, null);
    }

    @Unique
    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void tickFinal() {
        ((IStellarNetworkPowerManager) powerManager).finalApplyReceivedPower();
    }

}
