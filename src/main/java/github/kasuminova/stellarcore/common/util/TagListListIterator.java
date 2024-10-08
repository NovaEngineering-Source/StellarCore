package github.kasuminova.stellarcore.common.util;

import github.kasuminova.stellarcore.mixin.util.StellarNBTTagList;
import net.minecraft.nbt.NBTBase;
import net.minecraftforge.common.util.Constants;

import java.util.ListIterator;

public class TagListListIterator implements ListIterator<NBTBase> {

    private final ListIterator<NBTBase> parent;
    private final StellarNBTTagList changeHandler;

    public TagListListIterator(final ListIterator<NBTBase> parent, final StellarNBTTagList changeHandler) {
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
    public boolean hasPrevious() {
        return parent.hasPrevious();
    }

    @Override
    public NBTBase previous() {
        NBTBase prev = parent.previous();
        final byte id = prev.getId();
        if (changeHandler != null && (id == Constants.NBT.TAG_BYTE_ARRAY || (id >= Constants.NBT.TAG_LIST && id <= Constants.NBT.TAG_LONG_ARRAY))) {
            changeHandler.stellar_core$onModified(); // If tag cannot unique.
        }
        return prev;
    }

    @Override
    public int nextIndex() {
        return parent.nextIndex();
    }

    @Override
    public int previousIndex() {
        return parent.previousIndex();
    }

    @Override
    public void remove() {
        parent.remove();
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
    }

    @Override
    public void set(final NBTBase nbtBase) {
        parent.set(nbtBase);
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
    }

    @Override
    public void add(final NBTBase nbtBase) {
        parent.add(nbtBase);
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
    }

}
