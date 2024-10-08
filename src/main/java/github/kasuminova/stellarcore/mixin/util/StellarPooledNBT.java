package github.kasuminova.stellarcore.mixin.util;

import net.minecraft.nbt.NBTBase;

public interface StellarPooledNBT {

    NBTBase stellar_core$getPooledNBT();

    boolean stellar_core$isPooled();

    void stellar_core$setPooled(boolean pooled);

}
