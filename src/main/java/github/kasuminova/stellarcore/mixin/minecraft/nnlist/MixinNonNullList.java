package github.kasuminova.stellarcore.mixin.minecraft.nnlist;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.NonNullList;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
@Mixin(NonNullList.class)
public abstract class MixinNonNullList extends AbstractList {

    @Shadow @Final private List delegate;

    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void injectCreate(final CallbackInfoReturnable<NonNullList> cir) {
        cir.setReturnValue(new github.kasuminova.stellarcore.common.util.NonNullList());
    }

    @Inject(method = "withSize", at = @At("HEAD"), cancellable = true)
    private static void injectWithSize(final int size, final Object fill, final CallbackInfoReturnable<NonNullList> cir) {
        Validate.notNull(fill);
        Object[] aobject = new Object[size];
        Arrays.fill(aobject, fill);
        cir.setReturnValue(new github.kasuminova.stellarcore.common.util.NonNullList(new ObjectArrayList(aobject), fill));
    }

    @Inject(method = "from", at = @At("HEAD"), cancellable = true)
    private static void injectFrom(final Object defaultElementIn, final Object[] elements, final CallbackInfoReturnable<NonNullList> cir) {
        cir.setReturnValue(new github.kasuminova.stellarcore.common.util.NonNullList(new ObjectArrayList(elements), defaultElementIn));
    }

    @Nonnull
    @Override
    public Iterator iterator() {
        return delegate.iterator();
    }

    @Nonnull
    @Override
    public ListIterator listIterator() {
        return delegate.listIterator();
    }

    @Nonnull
    @Override
    public ListIterator listIterator(final int index) {
        return delegate.listIterator(index);
    }

}
