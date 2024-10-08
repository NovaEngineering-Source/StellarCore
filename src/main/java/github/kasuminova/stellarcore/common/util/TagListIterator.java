package github.kasuminova.stellarcore.common.util;

import github.kasuminova.stellarcore.mixin.util.StellarNBTTagList;
import net.minecraft.nbt.NBTBase;
import net.minecraftforge.common.util.Constants;

import java.util.Iterator;

public class TagListIterator implements Iterator<NBTBase> {

    private final Iterator<NBTBase> parent;
    private final StellarNBTTagList changeHandler;

    public TagListIterator(final Iterator<NBTBase> parent, final StellarNBTTagList changeHandler) {
        this.parent = parent;
        this.changeHandler = changeHandler;
    }

    @Override
    public boolean hasNext() {
        return parent.hasNext();
    }

    @Override
    public NBTBase next() {
        NBTBase next = parent.next();
        final byte id = next.getId();
        if (changeHandler != null && (id == Constants.NBT.TAG_BYTE_ARRAY || (id >= Constants.NBT.TAG_LIST && id <= Constants.NBT.TAG_LONG_ARRAY))) {
            changeHandler.stellar_core$onModified(); // If tag cannot unique.
        }
        return next;
    }

    @Override
    public void remove() {
        parent.remove();
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
    }

}
