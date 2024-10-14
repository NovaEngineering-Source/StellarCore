package github.kasuminova.stellarcore.mixin.util;

public interface IStellarNetworkPowerManager {

    void stellar_core$parallelTick();

    void stellar_core$finalApplyReceivedPower();

}
