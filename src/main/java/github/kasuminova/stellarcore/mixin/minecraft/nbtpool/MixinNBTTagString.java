package github.kasuminova.stellarcore.mixin.minecraft.nbtpool;

import github.kasuminova.stellarcore.mixin.util.StellarPooledNBT;
import net.minecraft.nbt.NBTTagString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;

@Mixin(NBTTagString.class)
public class MixinNBTTagString implements StellarPooledNBT {

    /**
     * @author Kasumi_Nova
     * @reason Constant Tag.
     */
    @Nonnull
    @Overwrite
    public NBTTagString copy() {
        return (NBTTagString) (Object) this;
    }

    @Override
    public Object stellar_core$getPooledNBT() {
        return copy();
    }

    /**
     * @author Kasumi_Nova
     * @reason Constant Tag.
     */
    @Inject(method = "equals", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("RedundantCast")
    public void equals(final Object obj, final CallbackInfoReturnable<Boolean> cir) {
        if (obj == (Object) this) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }

}
