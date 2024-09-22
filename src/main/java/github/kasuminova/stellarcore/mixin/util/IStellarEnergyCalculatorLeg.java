package github.kasuminova.stellarcore.mixin.util;

import ic2.core.energy.grid.Grid;

public interface IStellarEnergyCalculatorLeg {

    IC2EnergySyncCalcTask doParallelCalc(final Grid grid);
    
    void doSyncCalc(final IC2EnergySyncCalcTask task);

}
