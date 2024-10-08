package github.kasuminova.stellarcore.mixin.util;

import github.kasuminova.stellarcore.common.util.NBTTagBackingMap;

public interface StellarNBTTagCompound extends StellarNBTTagUnique {

    NBTTagBackingMap stellar_core$getTagMap();

    void stellar_core$setTagMap(final NBTTagBackingMap tagMap);

}
