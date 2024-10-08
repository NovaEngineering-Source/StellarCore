package github.kasuminova.stellarcore.mixin.util;

import net.minecraft.nbt.NBTTagCompound;

public interface StellarItemStack {

    void stellar_core$initCap();

    void stellar_core$joinCapInit();

    void stellar_core$ensureCapInitialized();

    void stellar_core$ensureCapNBTInitialized();

    NBTTagCompound stellar_core$getCapNBT();

}
