package github.kasuminova.stellarcore.mixin.minecraft.nbtpool;

import github.kasuminova.stellarcore.mixin.util.StellarPooledNBT;
import net.minecraft.nbt.NBTBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(NBTBase.class)
public class MixinNBTBase implements StellarPooledNBT {

    @Unique
    private boolean stellar_core$pooled = false;

    @Override
    public NBTBase stellar_core$getPooledNBT() {
        return (NBTBase) (Object) this;
    }

    @Override
    public boolean stellar_core$isPooled() {
        return stellar_core$pooled;
    }

    @Override
    public void stellar_core$setPooled(final boolean pooled) {
        this.stellar_core$pooled = pooled;
    }

}
