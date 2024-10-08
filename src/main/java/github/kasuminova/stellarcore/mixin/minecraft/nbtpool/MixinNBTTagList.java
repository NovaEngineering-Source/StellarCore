package github.kasuminova.stellarcore.mixin.minecraft.nbtpool;

import github.kasuminova.stellarcore.mixin.util.StellarPooledNBT;
import net.minecraft.nbt.NBTTagList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(NBTTagList.class)
public class MixinNBTTagList {

    /**
     * @author Kasumi_Nova
     * @reason Pooled values.
     */
    @Redirect(method = "read", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false))
    private boolean redirectRead(final List<Object> instance, final Object element) {
        return instance.add(((StellarPooledNBT) element).stellar_core$getPooledNBT());
    }

    @Redirect(method = "appendTag", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false))
    private boolean redirectAppendTag(final List<Object> instance, final Object element) {
        return instance.add(((StellarPooledNBT) element).stellar_core$getPooledNBT());
    }

    @Redirect(method = "set", at = @At(value = "INVOKE", target = "Ljava/util/List;set(ILjava/lang/Object;)Ljava/lang/Object;", remap = false))
    private Object redirectSet(final List<Object> instance, final int i, final Object element) {
        return instance.set(i, ((StellarPooledNBT) element).stellar_core$getPooledNBT());
    }

}
