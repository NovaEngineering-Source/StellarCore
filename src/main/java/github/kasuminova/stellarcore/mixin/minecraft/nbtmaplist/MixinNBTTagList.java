package github.kasuminova.stellarcore.mixin.minecraft.nbtmaplist;

import github.kasuminova.stellarcore.common.util.NBTTagBackingList;
import github.kasuminova.stellarcore.mixin.util.StellarNBTTagList;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(NBTTagList.class)
@SuppressWarnings({"MethodMayBeStatic", "RedundantCast"})
public class MixinNBTTagList implements StellarNBTTagList {

    @Unique
    private static final AtomicLong stellar_core$UID = new AtomicLong();

    @Unique
    private static final ThreadLocal<Boolean> stellar_core$CREATE_TAG_LIST = ThreadLocal.withInitial(() -> Boolean.TRUE);

    @Shadow
    private List<NBTBase> tagList;

    @Shadow
    private byte tagType;

    @Unique
    private long stellar_core$uid;

    @Unique
    private boolean stellar_core$unique = true;

    @Unique
    private boolean stellar_core$hashCached = false;

    @Unique
    private int stellar_core$hash = 0;

    // ===========================================================================
    // Unique Injections
    // ===========================================================================

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;",
                    remap = false
            )
    )
    private ArrayList<Object> injectInitNewArrayList() {
        return null;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        if (stellar_core$CREATE_TAG_LIST.get() == Boolean.TRUE) {
            stellar_core$setTagList(new NBTTagBackingList(4));
            this.stellar_core$uid = stellar_core$UID.incrementAndGet();
        }
    }

    @Inject(method = "read", at = @At("HEAD"))
    private void injectRead(final DataInput input, final int depth, final NBTSizeTracker sizeTracker, final CallbackInfo ci) {
        stellar_core$onModified();
    }

    @Redirect(method = "read", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayListWithCapacity(I)Ljava/util/ArrayList;", remap = false))
    private ArrayList<NBTBase> redirectNewArrayList(final int initialArraySize) {
        NBTTagBackingList list = new NBTTagBackingList(initialArraySize);
        list.setChangeHandler((StellarNBTTagList) (Object) this);
        return list;
    }

    @Redirect(method = {"get", "getCompoundTagAt", "getIntArrayAt"}, at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"))
    private Object injectGet(final List<NBTBase> instance, final int i) {
        final NBTBase tag = instance.get(i);
        final byte id = tag.getId();
        // TagByteArray, TagList, TagCompound, TagIntArray, TagLongArray
        if (id == Constants.NBT.TAG_BYTE_ARRAY || (id >= Constants.NBT.TAG_LIST && id <= Constants.NBT.TAG_LONG_ARRAY)) {
            stellar_core$onModified(); // If tag cannot unique.
        }
        return tag;
    }

    /**
     * @author Kasumi_Nova
     * @reason Use clone to copy tag faster.
     */
    @Nonnull
    @Overwrite
    public NBTTagList copy() {
        // Create NBTTagCompound without tagMap creation.
        stellar_core$CREATE_TAG_LIST.set(Boolean.FALSE);
        NBTTagList copied = new NBTTagList();
        stellar_core$CREATE_TAG_LIST.set(Boolean.TRUE);

        StellarNBTTagList accessor = (StellarNBTTagList) copied;
        // Set tagType.
        accessor.stellar_core$setTagType(this.tagType);
        // Copy UID.
        accessor.stellar_core$setUID(this.stellar_core$uid);
        accessor.stellar_core$setUnique(false);

        NBTTagBackingList copiedTagList = new NBTTagBackingList(this.tagList.size() + 1);
        // Copy tags, use unwrappedIterator to avoid trigger changeHandler.
        ((NBTTagBackingList) (this.tagList)).unwrappedIterator().forEachRemaining(tag -> copiedTagList.add(tag.copy()));
        // Overwrite tagMap.
        accessor.stellar_core$setTagList(copiedTagList);

        // Copy hashCode cache if present.
        if (this.stellar_core$hashCached) {
            accessor.stellar_core$setHashCodeCache(this.stellar_core$hash);
        }

        return copied;
    }

    @Inject(method = "equals", at = @At("HEAD"), cancellable = true)
    private void injectEquals(final Object obj, final CallbackInfoReturnable<Boolean> cir) {
        if (!(obj instanceof NBTTagList tag)) {
            cir.setReturnValue(Boolean.FALSE);
            return;
        }

        StellarNBTTagList accessor = (StellarNBTTagList) tag;
        if (this.tagType != accessor.stellar_core$getTagType()) {
            cir.setReturnValue(Boolean.FALSE);
            return;
        }

        if (!accessor.stellar_core$isUnique()) {
            if (this.stellar_core$uid == accessor.stellar_core$getUID()) {
                cir.setReturnValue(Boolean.TRUE);
                return;
            }
        }

        final boolean equals = tagList.equals(accessor.stellar_core$getTagList());
        if (equals && (!stellar_core$unique && !accessor.stellar_core$isUnique())) {
            // Merge uid.
            accessor.stellar_core$setUID(this.stellar_core$uid);
        }
        cir.setReturnValue(tagList.equals(accessor.stellar_core$getTagList()));
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
        return this.stellar_core$hash = super.hashCode() ^ this.tagList.hashCode();
    }

    @Override
    public NBTTagBackingList stellar_core$getTagList() {
        return (NBTTagBackingList) tagList;
    }

    @Override
    public void stellar_core$setTagList(final NBTTagBackingList tagList) {
        tagList.setChangeHandler((StellarNBTTagList) (Object) this);
        this.tagList = tagList;
    }

    @Override
    public byte stellar_core$getTagType() {
        return tagType;
    }

    @Override
    public void stellar_core$setTagType(final byte tagType) {
        this.tagType = tagType;
    }

    @Override
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
