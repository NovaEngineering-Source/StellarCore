package github.kasuminova.stellarcore.mixin.minecraft.nbtmap;

import github.kasuminova.stellarcore.common.util.NBTTagBackingMap;
import github.kasuminova.stellarcore.mixin.util.StellarNBTTagCompound;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(NBTTagCompound.class)
public abstract class MixinNBTTagCompound extends NBTBase implements StellarNBTTagCompound {

    @Unique
    private static final AtomicLong stellar_core$UID = new AtomicLong();

    @Unique
    private static final ThreadLocal<Boolean> stellar_core$CREATE_TAG_MAP = ThreadLocal.withInitial(() -> Boolean.TRUE);

    @Mutable
    @Shadow
    @Final
    private Map<String, NBTBase> tagMap;

    @Unique
    private long stellar_core$uid;

    @Unique
    private boolean stellar_core$unique = true;

    @Unique
    private boolean stellar_core$hashCached = false;

    @Unique
    private int stellar_core$hash = 0;

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;",
                    remap = false
            )
    )
    private HashMap<String, NBTBase> injectInitNewHashMap() {
        return null;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        if (stellar_core$CREATE_TAG_MAP.get() == Boolean.TRUE) {
            stellar_core$setTagMap(new NBTTagBackingMap(8));
            this.stellar_core$uid = stellar_core$UID.incrementAndGet();
        }
    }

    @Inject(method = "read", at = @At("HEAD"))
    private void injectRead(final DataInput input, final int depth, final NBTSizeTracker sizeTracker, final CallbackInfo ci) {
        stellar_core$onTagMapModified();
    }

    /**
     * @author Kasumi_Nova
     * @reason Ensure unique.
     */
    @Overwrite
    public NBTBase getTag(String key) {
        NBTBase ret = this.tagMap.get(key);
        if (ret == null) {
            return null;
        }

        byte id = ret.getId();
        // ByteArray, TagList, TagCompound, IntArray, LongArray
        if (id == Constants.NBT.TAG_BYTE_ARRAY || id >= Constants.NBT.TAG_LIST && id <= Constants.NBT.TAG_LONG_ARRAY) {
            // If tag cannot unique.
            stellar_core$onTagMapModified();
        }

        return ret;
    }

    /**
     * @author Kasumi_Nova
     * @reason Ensure unique.
     */
    @Overwrite
    public void removeTag(String key) {
        if (this.tagMap.remove(key) != null) {
            stellar_core$onTagMapModified();
        }
    }

    /**
     * @author Kasumi_Nova
     * @reason Ensure unique.
     */
    @Redirect(method = {"getByteArray", "getIntArray", "getCompoundTag", "getTagList"}, at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object redirectMapGet(final Map<?, ?> instance, final Object key) {
        Object ret = instance.get(key);
        if (ret != null) {
            // If tag cannot be unique.
            stellar_core$onTagMapModified();
        }
        return ret;
    }

    /**
     * @author Kasumi_Nova
     * @reason Ensure unique.
     */
    @Inject(method = "merge", at = @At("HEAD"))
    private void injectMerge(final NBTTagCompound other, final CallbackInfo ci) {
        stellar_core$onTagMapModified();
    }

    /**
     * @author Kasumi_Nova
     * @reason Use clone to copy tag faster.
     */
    @Nonnull
    @Overwrite
    public NBTTagCompound copy() {
        // Create NBTTagCompound without tagMap creation.
        stellar_core$CREATE_TAG_MAP.set(Boolean.FALSE);
        NBTTagCompound copied = new NBTTagCompound();
        stellar_core$CREATE_TAG_MAP.set(Boolean.TRUE);

        StellarNBTTagCompound accessor = (StellarNBTTagCompound) copied;
        accessor.stellar_core$setUID(this.stellar_core$getUID());
        accessor.stellar_core$setUnique(false);

        // Overwrite tagMap.
        NBTTagBackingMap cloned = ((NBTTagBackingMap) this.tagMap).clone();
        accessor.stellar_core$setTagMap(cloned);

        // Copy values.
        for (final Object2ObjectMap.Entry<String, NBTBase> entry : cloned.object2ObjectEntrySet()) {
            entry.setValue(entry.getValue().copy());
        }

        return copied;
    }

    @Inject(method = "equals", at = @At("HEAD"), cancellable = true)
    private void injectEquals(final Object obj, final CallbackInfoReturnable<Boolean> cir) {
        if (!(obj instanceof NBTTagCompound tag)) {
            cir.setReturnValue(Boolean.FALSE);
            return;
        }

        StellarNBTTagCompound accessor = (StellarNBTTagCompound) tag;
        if (!accessor.stellar_core$isUnique()) {
            if (stellar_core$uid == accessor.stellar_core$getUID()) {
                cir.setReturnValue(Boolean.TRUE);
                return;
            }
        }

        cir.setReturnValue(tagMap.equals(accessor.stellar_core$getTagMap()) ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * @author Kasumi_Nova
     * @reason hashCode Cache
     */
    @Overwrite
    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    public int hashCode() {
        if (this.stellar_core$hashCached) {
            return stellar_core$hash;
        }
        this.stellar_core$hashCached = true;
        return this.stellar_core$hash = super.hashCode() ^ this.tagMap.hashCode();
    }

    @Unique
    private void stellar_core$onTagMapModified() {
        if (!this.stellar_core$unique) {
            this.stellar_core$unique = true;
            this.stellar_core$uid = stellar_core$UID.incrementAndGet();
        }
        if (this.stellar_core$hashCached) {
            this.stellar_core$hashCached = false;
        }
    }

    @Unique
    @Override
    public NBTTagBackingMap stellar_core$getTagMap() {
        return (NBTTagBackingMap) tagMap;
    }

    @Unique
    @Override
    public void stellar_core$setTagMap(final NBTTagBackingMap tagMap) {
        tagMap.setOnChanged(this::stellar_core$onTagMapModified);
        this.tagMap = tagMap;
    }

    @Unique
    @Override
    public void stellar_core$setUID(final long uid) {
        this.stellar_core$uid = uid;
    }

    @Unique
    @Override
    public long stellar_core$getUID() {
        return this.stellar_core$uid;
    }

    @Unique
    @Override
    public void stellar_core$setUnique(final boolean unique) {
        this.stellar_core$unique = unique;
    }

    @Unique
    @Override
    public boolean stellar_core$isUnique() {
        return this.stellar_core$unique;
    }

}
