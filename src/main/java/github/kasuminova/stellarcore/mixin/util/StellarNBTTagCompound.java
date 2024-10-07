package github.kasuminova.stellarcore.mixin.util;

import github.kasuminova.stellarcore.common.util.NBTTagBackingMap;

public interface StellarNBTTagCompound {

    NBTTagBackingMap stellar_core$getTagMap();

    void stellar_core$setTagMap(final NBTTagBackingMap tagMap);

    void stellar_core$setUID(final long uid);

    long stellar_core$getUID();

    void stellar_core$setUnique(final boolean unique);

    boolean stellar_core$isUnique();

}
