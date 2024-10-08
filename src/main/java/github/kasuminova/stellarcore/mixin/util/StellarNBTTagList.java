package github.kasuminova.stellarcore.mixin.util;

import github.kasuminova.stellarcore.common.util.NBTTagBackingList;

public interface StellarNBTTagList extends StellarNBTTagUnique {

    NBTTagBackingList stellar_core$getTagList();

    void stellar_core$setTagList(NBTTagBackingList tagList);

    byte stellar_core$getTagType();

    void stellar_core$setTagType(byte tagType);

}
