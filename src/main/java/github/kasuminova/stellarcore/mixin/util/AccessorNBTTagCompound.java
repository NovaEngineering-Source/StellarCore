package github.kasuminova.stellarcore.mixin.util;

import net.minecraft.nbt.NBTBase;

import java.util.Map;

public interface AccessorNBTTagCompound {

    Map<String, NBTBase> stellar_core$getTagMap();

    void stellar_core$setTagMap(final Map<String, NBTBase> tagMap);

}
