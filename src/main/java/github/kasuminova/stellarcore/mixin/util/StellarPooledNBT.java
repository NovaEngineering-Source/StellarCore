package github.kasuminova.stellarcore.mixin.util;

import net.minecraft.nbt.NBTBase;

public interface StellarPooledNBT {

    static Object stellar_core$getPooledNBT(final NBTBase nbt) {
        try {
            return ((StellarPooledNBT) nbt).stellar_core$getPooledNBT();
        } catch (ClassCastException e) {
            return nbt;
        }
    }

    /**
     * 返回 Object 来兼容其他模组的反射。
     */
    Object stellar_core$getPooledNBT();

}
