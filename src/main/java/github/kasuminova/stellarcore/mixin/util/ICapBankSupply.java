package github.kasuminova.stellarcore.mixin.util;

import crazypants.enderio.base.power.IPowerStorage;

import java.util.Set;

public interface ICapBankSupply {

    int getCanExtract();

    int getCanFill();

    Set<IPowerStorage> getCapBanks();

    void invokeInit();

    void invokeBalance();

    void invokeRemove(long remove);

    long invokeAdd(long add);

}
