package github.kasuminova.stellarcore.mixin.minecraft.nbtmaplist;

import github.kasuminova.stellarcore.common.util.NBTTagBackingMap;
import github.kasuminova.stellarcore.mixin.util.StellarNBTTagCompound;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.crash.CrashReport;
import net.minecraft.nbt.*;
import net.minecraft.util.ReportedException;
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

    @Shadow
    public abstract byte getTagId(final String key);

    @Shadow
    protected abstract CrashReport createCrashReport(final String key, final int expectedType, final ClassCastException ex);

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

    // ===========================================================================
    // Unique Injections
    // ===========================================================================

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        if (stellar_core$CREATE_TAG_MAP.get() == Boolean.TRUE) {
            stellar_core$setTagMap(new NBTTagBackingMap());
            this.stellar_core$uid = stellar_core$UID.incrementAndGet();
        }
    }

    @Inject(method = "read", at = @At("HEAD"))
    private void injectRead(final DataInput input, final int depth, final NBTSizeTracker sizeTracker, final CallbackInfo ci) {
        stellar_core$onModified();
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
            stellar_core$onModified();
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
            stellar_core$onModified();
        }
    }

    /**
     * @author Kasumi_Nova
     * @reason Ensure unique.
     */
    @Inject(method = "merge", at = @At("HEAD"))
    private void injectMerge(final NBTTagCompound other, final CallbackInfo ci) {
        stellar_core$onModified();
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

        // Copy UID.
        StellarNBTTagCompound accessor = (StellarNBTTagCompound) copied;
        accessor.stellar_core$setUID(this.stellar_core$uid);
        accessor.stellar_core$setUnique(false);

        // Overwrite tagMap.
        NBTTagBackingMap cloned = ((NBTTagBackingMap) this.tagMap).clone();
        accessor.stellar_core$setTagMap(cloned);

        // Copy hashCode cache if present.
        if (this.stellar_core$hashCached) {
            accessor.stellar_core$setHashCodeCache(this.stellar_core$hash);
        }

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
            if (this.stellar_core$uid == accessor.stellar_core$getUID()) {
                cir.setReturnValue(Boolean.TRUE);
                return;
            }
        }

        final boolean equals = tagMap.equals(accessor.stellar_core$getTagMap());
        if (equals && (!stellar_core$unique && !accessor.stellar_core$isUnique())) {
            // Merge uid.
            accessor.stellar_core$setUID(this.stellar_core$uid);
        }
        cir.setReturnValue(equals);
    }

    /**
     * @author Kasumi_Nova
     * @reason hashCode Cache
     */
    @Overwrite
    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    public int hashCode() {
        if (this.stellar_core$hashCached) {
            return this.stellar_core$hash;
        }
        this.stellar_core$hashCached = true;
        return this.stellar_core$hash = super.hashCode() ^ this.tagMap.hashCode();
    }

    // ===========================================================================
    // Get Improvements
    // ===========================================================================

    /**
     * @author Kasumi_Nova
     * @reason Get improvements.
     */
    @Overwrite
    public boolean hasKey(String key, int requiredType) {
        int id = this.getTagId(key);

        // 1 to 6 are valid number types.
        return id == requiredType || (requiredType == Constants.NBT.TAG_ANY_NUMERIC && id >= Constants.NBT.TAG_BYTE && id <= Constants.NBT.TAG_DOUBLE);
    }

    /**
     * @author Kasumi_Nova
     * @reason Inline type check.
     */
    @Overwrite
    public byte getByte(String key) {
        try {
            final NBTBase tag = getTag(key);
            if (tag == null) {
                return 0;
            }

            final byte id = tag.getId();
            if (id >= Constants.NBT.TAG_BYTE && id <= Constants.NBT.TAG_DOUBLE) {
                return ((NBTPrimitive) tag).getByte();
            }
        } catch (ClassCastException ignored) {
        }

        return 0;
    }

    /**
     * @author Kasumi_Nova
     * @reason Inline type check.
     */
    @Overwrite
    public short getShort(String key) {
        try {
            final NBTBase tag = getTag(key);
            if (tag == null) {
                return 0;
            }

            final byte id = tag.getId();
            if (id >= Constants.NBT.TAG_BYTE && id <= Constants.NBT.TAG_DOUBLE) {
                return ((NBTPrimitive) tag).getShort();
            }
        } catch (ClassCastException ignored) {
        }
        return 0;
    }

    /**
     * @author Kasumi_Nova
     * @reason Inline type check.
     */
    @Overwrite
    public int getInteger(String key) {
        try {
            final NBTBase tag = getTag(key);
            if (tag == null) {
                return 0;
            }

            final byte id = tag.getId();
            if (id >= Constants.NBT.TAG_BYTE && id <= Constants.NBT.TAG_DOUBLE) {
                return ((NBTPrimitive) tag).getInt();
            }
        } catch (ClassCastException ignored) {
        }
        return 0;
    }

    /**
     * @author Kasumi_Nova
     * @reason Inline type check.
     */
    @Overwrite
    public long getLong(String key) {
        try {
            final NBTBase tag = getTag(key);
            if (tag == null) {
                return 0;
            }

            final byte id = tag.getId();
            if (id >= Constants.NBT.TAG_BYTE && id <= Constants.NBT.TAG_DOUBLE) {
                return ((NBTPrimitive) tag).getLong();
            }
        } catch (ClassCastException ignored) {
        }
        return 0;
    }

    /**
     * @author Kasumi_Nova
     * @reason Inline type check.
     */
    @Overwrite
    public float getFloat(String key) {
        try {
            final NBTBase tag = getTag(key);
            if (tag == null) {
                return 0F;
            }

            final byte id = tag.getId();
            if (id >= Constants.NBT.TAG_BYTE && id <= Constants.NBT.TAG_DOUBLE) {
                return ((NBTPrimitive) tag).getFloat();
            }
        } catch (ClassCastException ignored) {
        }
        return 0F;
    }

    /**
     * @author Kasumi_Nova
     * @reason Inline type check.
     */
    @Overwrite
    public double getDouble(String key) {
        try {
            final NBTBase tag = getTag(key);
            if (tag == null) {
                return 0D;
            }

            final byte id = tag.getId();
            if (id >= Constants.NBT.TAG_BYTE && id <= Constants.NBT.TAG_DOUBLE) {
                return ((NBTPrimitive) tag).getDouble();
            }
        } catch (ClassCastException ignored) {
        }
        return 0D;
    }

    /**
     * @author Kasumi_Nova
     * @reason Inline type check.
     */
    @Overwrite
    public String getString(String key) {
        try {
            final NBTBase tag = getTag(key);
            if (tag == null) {
                return "";
            }

            if (tag.getId() == Constants.NBT.TAG_STRING) {
                return ((NBTTagString) tag).getString();
            }
        } catch (ClassCastException ignored) {
        }

        return "";
    }

    /**
     * @author Kasumi_Nova
     * @reason Inline type check.
     */
    @Overwrite
    public byte[] getByteArray(String key) {
        try {
            final NBTBase tag = getTag(key);
            if (tag == null) {
                return new byte[0];
            }

            if (tag.getId() == Constants.NBT.TAG_BYTE_ARRAY) {
                stellar_core$onModified(); // Mark as changed because array is mutable.
                return ((NBTTagByteArray) tag).getByteArray();
            }
        } catch (ClassCastException ex) {
            throw new ReportedException(this.createCrashReport(key, Constants.NBT.TAG_BYTE_ARRAY, ex));
        }

        return new byte[0];
    }

    /**
     * @author Kasumi_Nova
     * @reason Inline type check.
     */
    @Overwrite
    public int[] getIntArray(String key) {
        try {
            final NBTBase tag = getTag(key);
            if (tag == null) {
                return new int[0];
            }

            if (tag.getId() == Constants.NBT.TAG_INT_ARRAY) {
                stellar_core$onModified(); // Mark as changed because array is mutable.
                return ((NBTTagIntArray) tag).getIntArray();
            }
        } catch (ClassCastException ex) {
            throw new ReportedException(this.createCrashReport(key, Constants.NBT.TAG_INT_ARRAY, ex));
        }
        
        return new int[0];
    }

    /**
     * @author Kasumi_Nova
     * @reason Inline type check.
     */
    @Overwrite
    public NBTTagCompound getCompoundTag(String key) {
        try {
            final NBTBase tag = getTag(key);
            if (tag == null) {
                return new NBTTagCompound();
            }

            if (tag.getId() == Constants.NBT.TAG_COMPOUND) {
                stellar_core$onModified(); // Mark as changed because NBTTagCompound is mutable.
                return (NBTTagCompound) tag;
            }
        } catch (ClassCastException ex) {
            throw new ReportedException(this.createCrashReport(key, Constants.NBT.TAG_COMPOUND, ex));
        }

        return new NBTTagCompound();
    }

    /**
     * @author Kasumi_Nova
     * @reason Inline type check.
     */
    @Overwrite
    public NBTTagList getTagList(String key, int type) {
        try {
            final NBTBase tag = getTag(key);
            if (tag == null) {
                return new NBTTagList();
            }

            if (tag.getId() == Constants.NBT.TAG_LIST) {
                final NBTTagList list = (NBTTagList) tag;
                if (!list.isEmpty() && list.getTagType() != type) {
                    return new NBTTagList();
                }
                stellar_core$onModified(); // Mark as changed because NBTTagList is mutable.
                return list;
            }
        } catch (ClassCastException ex) {
            throw new ReportedException(this.createCrashReport(key, Constants.NBT.TAG_LIST, ex));
        }

        return new NBTTagList();
    }

    @Unique
    public void stellar_core$onModified() {
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
    @SuppressWarnings("RedundantCast")
    public void stellar_core$setTagMap(final NBTTagBackingMap tagMap) {
        tagMap.setChangeHandler((StellarNBTTagCompound) (Object) this);
        this.tagMap = tagMap;
    }

    @Unique
    @Override
    public void stellar_core$setUID(final long uid) {
        this.stellar_core$uid = uid;
        this.stellar_core$unique = false;
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
    public Boolean stellar_core$isUnique() {
        return this.stellar_core$unique;
    }

    @Override
    public void stellar_core$setHashCodeCache(final int hashCode) {
        this.stellar_core$hashCached = true;
        this.stellar_core$hash = hashCode;
    }

}
