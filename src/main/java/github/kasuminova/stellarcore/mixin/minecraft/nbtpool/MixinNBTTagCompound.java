package github.kasuminova.stellarcore.mixin.minecraft.nbtpool;

import github.kasuminova.stellarcore.common.pool.NBTTagPrimitivePool;
import github.kasuminova.stellarcore.mixin.util.StellarPooledNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(NBTTagCompound.class)
public class MixinNBTTagCompound implements StellarPooledNBT {

    @Final
    @Shadow
    private Map<String, NBTBase> tagMap;

    /**
     * @author Kasumi_Nova
     * @reason Pooled values.
     */
    @SuppressWarnings("MethodMayBeStatic")
    @Redirect(method = "read", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", remap = false))
    private Object redirectRead(final Map<Object, Object> instance, final Object key, final Object value) {
        return instance.put(key, StellarPooledNBT.stellar_core$getPooledNBT((NBTBase) value));
    }

    /**
     * @author Kasumi_Nova
     * @reason Pooled values.
     */
    @Overwrite
    public void setTag(String key, NBTBase value) {
        if (value == null) {
            throw new IllegalArgumentException("Invalid null NBT value with key " + key);
        }
        this.tagMap.put(key, (NBTBase) StellarPooledNBT.stellar_core$getPooledNBT(value));
    }

    /**
     * @author Kasumi_Nova
     * @reason Pooled values.
     */
    @Overwrite
    public void setByte(String key, byte value) {
        this.tagMap.put(key, NBTTagPrimitivePool.getTagByte(value));
    }

    /**
     * @author Kasumi_Nova
     * @reason Pooled values.
     */
    @Overwrite
    public void setShort(String key, short value) {
        this.tagMap.put(key, NBTTagPrimitivePool.getTagShort(value));
    }

    /**
     * @author Kasumi_Nova
     * @reason Pooled values.
     */
    @Overwrite
    public void setInteger(String key, int value) {
        this.tagMap.put(key, NBTTagPrimitivePool.getTagInt(value));
    }

    /**
     * @author Kasumi_Nova
     * @reason Pooled values.
     */
    @Overwrite
    public void setLong(String key, long value) {
        this.tagMap.put(key, NBTTagPrimitivePool.getTagLong(value));
    }

    /**
     * @author Kasumi_Nova
     * @reason Pooled values.
     */
    @Overwrite
    public void setFloat(String key, float value) {
        this.tagMap.put(key, NBTTagPrimitivePool.getTagFloat(value));
    }

    /**
     * @author Kasumi_Nova
     * @reason Pooled values.
     */
    @Overwrite
    public void setDouble(String key, double value) {
        this.tagMap.put(key, NBTTagPrimitivePool.getTagDouble(value));
    }

    @SuppressWarnings("RedundantCast")
    @Override
    public Object stellar_core$getPooledNBT() {
        return (Object) this;
    }

}
