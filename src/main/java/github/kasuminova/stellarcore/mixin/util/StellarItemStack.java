package github.kasuminova.stellarcore.mixin.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;

public interface StellarItemStack {

    void stellar_core$initCap();

    void stellar_core$joinCapInit();

    void stellar_core$ensureCapInitialized();

    void stellar_core$ensureCapNBTInitialized();

    NBTTagCompound stellar_core$getCapNBT();

    CapabilityDispatcher stellar_core$getCap();

}
