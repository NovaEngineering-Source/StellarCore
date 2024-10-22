package github.kasuminova.stellarcore.common.util;

import github.kasuminova.stellarcore.mixin.util.StellarNBTTagCompound;
import github.kasuminova.stellarcore.mixin.util.TagKeySet;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.nbt.NBTBase;

import javax.annotation.Nonnull;
import java.util.Map;

@SuppressWarnings("CloneableClassInSecureContext")
public class NBTTagBackingMap extends Object2ObjectOpenHashMap<String, NBTBase> {

    private boolean changed = false;
    private StellarNBTTagCompound changeHandler = null;

    public NBTTagBackingMap(final int expected) {
        super(expected);
    }

    public NBTTagBackingMap() {
        super(4);
    }

    public NBTTagBackingMap(final Map<String, NBTBase> m) {
        super(m);
    }

    public NBTTagBackingMap(final Object2ObjectMap<String, NBTBase> m) {
        super(m);
    }

    public void setChangeHandler(final StellarNBTTagCompound changeHandler) {
        this.changed = false;
        this.changeHandler = changeHandler;
    }

    @Override
    public NBTBase put(final String k, final NBTBase v) {
        if (!changed) {
            if (changeHandler != null) {
                changeHandler.stellar_core$onModified();
            }
        }
        return super.put(k, v);
    }

    @Override
    public NBTBase remove(final Object k) {
        if (!changed) {
            if (changeHandler != null) {
                changeHandler.stellar_core$onModified();
            }
        }
        return super.remove(k);
    }

    @Nonnull
    @Override
    public ObjectSet<String> keySet() {
        return new TagKeySet((AbstractObjectSet<String>) super.keySet(), changeHandler);
    }

    @Override
    @SuppressWarnings("NonFinalClone")
    public NBTTagBackingMap clone() {
        return (NBTTagBackingMap) super.clone();
    }

}
