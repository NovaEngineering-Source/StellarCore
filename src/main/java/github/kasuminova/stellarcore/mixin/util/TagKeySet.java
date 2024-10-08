package github.kasuminova.stellarcore.mixin.util;

import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import javax.annotation.Nonnull;

@SuppressWarnings("CloneableClassInSecureContext")
public class TagKeySet extends AbstractObjectSet<String> {

    private final AbstractObjectSet<String> parent;
    private final StellarNBTTagCompound changeHandler;

    public TagKeySet(final AbstractObjectSet<String> parent, final StellarNBTTagCompound changeHandler) {
        this.parent = parent;
        this.changeHandler = changeHandler;
    }

    @Nonnull
    @Override
    public ObjectIterator<String> iterator() {
        return new TagKeyIterator(parent.iterator());
    }

    @Override
    public int size() {
        return parent.size();
    }

    @Override
    public boolean rem(final Object key) {
        boolean removed = super.rem(key);
        if (removed) {
            if (changeHandler != null) {
                changeHandler.stellar_core$onModified();
            }
        }
        return removed;
    }

    @Override
    public void clear() {
        super.clear();
        if (changeHandler != null) {
            changeHandler.stellar_core$onModified();
        }
    }

    private class TagKeyIterator implements ObjectIterator<String> {

        private final ObjectIterator<String> parent;

        private TagKeyIterator(final ObjectIterator<String> parent) {
            this.parent = parent;
        }

        @Override
        public void remove() {
            parent.remove();
            if (changeHandler != null) {
                changeHandler.stellar_core$onModified();
            }
        }

        @Override
        public String next() {
            return parent.next();
        }

        @Override
        public boolean hasNext() {
            return parent.hasNext();
        }

        @Override
        public int skip(final int n) {
            return parent.skip(n);
        }

    }

}
