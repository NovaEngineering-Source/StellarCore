package github.kasuminova.stellarcore.common.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nullable;
import java.util.List;

public class NonNullList<E> extends net.minecraft.util.NonNullList<E> {

    public NonNullList() {
        this(new ObjectArrayList<>(), null);
    }

    public NonNullList(final List<E> delegateIn, @Nullable final E listType) {
        super(delegateIn, listType);
    }

}
