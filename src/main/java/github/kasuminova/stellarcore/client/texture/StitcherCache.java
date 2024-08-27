package github.kasuminova.stellarcore.client.texture;

import github.kasuminova.stellarcore.StellarCore;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.LargeNBTUtils;
import github.kasuminova.stellarcore.mixin.minecraft.stitcher.AccessorStitcher;
import github.kasuminova.stellarcore.mixin.util.AccessorStitcherHolder;
import github.kasuminova.stellarcore.mixin.util.AccessorStitcherSlot;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

public class StitcherCache {

    private static final String CACHE_FILE_NAME = "config" + File.separator + "stellarcore_stitcher_cache_{}.dat";
    private static final Map<TextureMap, StitcherCache> CREATED_STITCHER_CACHE = new Reference2ObjectOpenHashMap<>();

    private static TextureMap activeMapToStitch = null;

    private final String name;
    private final File cacheFile;
    private final Map<String, Stitcher.Holder> holders = new Object2ObjectOpenHashMap<>();
    private final List<Stitcher.Slot> slots = new ObjectArrayList<>();
    private final TextureMap cacheFor;

    private Future<Void> readTask;

    private NBTTagCompound readTag = null;

    private int width = 0;
    private int height = 0;

    private State cacheState = State.UNKNOWN;

    private StitcherCache(final String name, final TextureMap cacheFor) {
        this.name = name;
        this.cacheFile = new File(CACHE_FILE_NAME.replace("{}", name));
        this.cacheFor = cacheFor;
        this.readTask = CompletableFuture.runAsync(this::readFromFile);
        StellarCore.log.info("[StellarCore-StitcherCache] Created stitcher cache for `{}`.", cacheFor.getBasePath());
        StellarCore.log.info("[StellarCore-StitcherCache] Stitcher file cache `{}` reader task started.", cacheFor.getBasePath());
    }

    public static StitcherCache create(final String name, final TextureMap cacheFor) {
        StitcherCache cache = CREATED_STITCHER_CACHE.get(cacheFor);
        if (cache != null) {
            cache.checkReadTaskState();
            cache.readTask = CompletableFuture.runAsync(cache::readFromFile);
            StellarCore.log.info("[StellarCore-StitcherCache] Stitcher file cache `{}` reader task restarted.", cacheFor.getBasePath());
            return cache;
        }
        cache = new StitcherCache(name, cacheFor);
        CREATED_STITCHER_CACHE.put(cacheFor, cache);
        return cache;
    }

    public static StitcherCache getActiveCache() {
        if (activeMapToStitch == null) {
            return null;
        }
        return CREATED_STITCHER_CACHE.get(activeMapToStitch);
    }

    public static void setActiveMap(final TextureMap activeMap) {
        StitcherCache.activeMapToStitch = activeMap;
    }

    public void writeToFile() {
        try {
            if (cacheFile.exists() && !cacheFile.delete()) {
                throw new IOException("Cannot delete file " + cacheFile.getAbsolutePath());
            }
            if (!cacheFile.createNewFile()) {
                throw new IOException("Cannot create file " + cacheFile.getAbsolutePath());
            }

            FileOutputStream fos = new FileOutputStream(cacheFile);
            CompressedStreamTools.writeCompressed(toNBT(), fos);
            fos.close();
            StellarCore.log.info("[StellarCore-StitcherCache] Successfully write stitcher cache file to `{}`.", cacheFile.getAbsolutePath());
        } catch (Throwable e) {
            StellarCore.log.error("[StellarCore-StitcherCache] Failed to write stitcher cache file! Please report it.", e);
        }
    }

    public void readFromFile() {
        if (!cacheFile.exists()) {
            this.cacheState = State.UNAVAILABLE;
            StellarCore.log.info("[StellarCore-StitcherCache] Stitcher cache file is unavailable (File not found).");
            return;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(cacheFile);
            readTag = LargeNBTUtils.readCompressed(fis);
            fis.close();
            this.cacheState = State.TAG_READY;
            StellarCore.log.info("[StellarCore-StitcherCache] Successfully read stitcher cache file from `{}`.", cacheFile.getAbsolutePath());
        } catch (Throwable e) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Throwable ignored) {
                }
            }
            this.cacheState = State.UNAVAILABLE;
            StellarCore.log.warn("[StellarCore-StitcherCache] Failed to read stitcher cache file, it may be broken.", e);
        }
    }

    public void parseTag(final Stitcher stitcher, final Set<Stitcher.Holder> targetHolders) {
        checkReadTaskState();

        if (cacheState != State.TAG_READY) {
            return;
        }

        try {
            fromNBT(readTag, stitcher);
            this.cacheState = holdersEquals(targetHolders) ? State.AVAILABLE : State.UNAVAILABLE;
            StellarCore.log.info("[StellarCore-StitcherCache] Stitcher cache parsed, state: {}.", this.cacheState);
        } catch (Throwable e) {
            StellarCore.log.warn("[StellarCore-StitcherCache] Failed to parse stitcher cache file, it may be broken.", e);
        }
    }

    public boolean holdersEquals(Set<Stitcher.Holder> targetHolders) {
        if (targetHolders.size() != this.holders.size()) {
            StellarCore.log.warn("[StellarCore-StitcherCache] Stitcher cache may be unavailable, holders size not equals ({} ≠ {}).", targetHolders.size(), this.holders.size());
//            return false;
        }
        Map<String, Stitcher.Holder> holders = new Object2ObjectOpenHashMap<>(this.holders);
        for (final Stitcher.Holder holder : targetHolders) {
            Stitcher.Holder cachedHolder = holders.get(holder.getAtlasSprite().getIconName());
            if (cachedHolder == null || !holderEquals(cachedHolder, holder)) {
                if (cachedHolder == null) {
                    StellarCore.log.warn("[StellarCore-StitcherCache] Stitcher cache is unavailable, holder `{}` not found in cache.", holder.getAtlasSprite().getIconName());
                } else {
                    StellarCore.log.warn("[StellarCore-StitcherCache] Stitcher cache is unavailable, holder `{}` not equals.", holder.getAtlasSprite().getIconName());
                }
                return false;
            }
            holders.remove(holder.getAtlasSprite().getIconName());
        }
        if (!holders.isEmpty()) {
            StellarCore.log.warn("[StellarCore-StitcherCache] Stitcher cache is unavailable, {} holders not found in runtime.", holders.size());
            if (StellarCoreConfig.DEBUG.enableDebugLog) {
                holders.keySet().forEach(holderName -> StellarCore.log.warn("[StellarCore-StitcherCache] Holder `{}` not found in runtime.", holderName));
            }
            return false;
        }
        return true;
    }

    public void cache(Set<Stitcher.Holder> holders, List<Stitcher.Slot> slots, int width, int height) {
        this.holders.clear();
        this.slots.clear();
        holders.forEach(holder -> this.holders.put(holder.getAtlasSprite().getIconName(), holder));
        this.slots.addAll(slots);
        this.width = width;
        this.height = height;
        this.cacheState = State.AVAILABLE;
    }

    public String getName() {
        return name;
    }

    public File getCacheFile() {
        return cacheFile;
    }

    public State getCacheState() {
        return cacheState;
    }

    public TextureMap getCacheFor() {
        return cacheFor;
    }

    public List<Stitcher.Slot> getSlots() {
        return slots;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();

        NBTTagList holdersTag = new NBTTagList();
        holders.values().stream()
                .map(this::writeHolderNBT)
                .forEach(holdersTag::appendTag);
        tag.setTag("holders", holdersTag);

        NBTTagList slotsTag = new NBTTagList();
        slots.stream()
                .map(StitcherCache::writeSlotNBT)
                .forEach(slotsTag::appendTag);
        tag.setTag("slots", slotsTag);

        tag.setInteger("width", width);
        tag.setInteger("height", height);

        return tag;
    }

    private NBTTagCompound writeHolderNBT(final Stitcher.Holder holder) {
        NBTTagCompound holderTag = new NBTTagCompound();
        holderTag.setString("sprite", holder.getAtlasSprite().getIconName());
        holderTag.setBoolean("rotated", holder.isRotated());
        if (holder.getAtlasSprite() == cacheFor.getMissingSprite()) {
            holderTag.setBoolean("empty", true);
        }
        return holderTag;
    }

    private static NBTTagCompound writeSlotNBT(Stitcher.Slot slot) {
        AccessorStitcherSlot slotAccessor = (AccessorStitcherSlot) slot;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("originX", slot.getOriginX());
        tag.setInteger("originY", slot.getOriginY());
        tag.setInteger("width", slotAccessor.width());
        tag.setInteger("height", slotAccessor.height());

        NBTTagList subSlotsTag = new NBTTagList();
        List<Stitcher.Slot> subSlots = slotAccessor.subSlots();
        if (subSlots != null && !subSlots.isEmpty()) {
            subSlots.stream()
                    .map(StitcherCache::writeSlotNBT)
                    .forEach(subSlotsTag::appendTag);
        }

        tag.setTag("subSlots", subSlotsTag);
        Stitcher.Holder holder = slot.getStitchHolder();
        //noinspection ConstantValue
        if (holder == null) {
            tag.setBoolean("holderEmpty", true);
        } else {
            tag.setString("holder", holder.getAtlasSprite().getIconName());
        }
        return tag;
    }

    private void checkReadTaskState() {
        if (readTask != null) {
            if (!readTask.isDone()) {
                try {
                    readTask.get();
                } catch (Throwable e) {
                    StellarCore.log.error("[StellarCore-StitcherCache] Failed to read stitcher cache file! Please report it.", e);
                }
            }
        }
    }

    private void fromNBT(NBTTagCompound tag, Stitcher stitcher) {
        this.holders.clear();
        this.slots.clear();

        NBTTagList holdersTagList = tag.getTagList("holders", Constants.NBT.TAG_COMPOUND);
        IntStream.range(0, holdersTagList.tagCount()).mapToObj(holdersTagList::getCompoundTagAt).forEach(holderTag -> {
            Stitcher.Holder holder = readHolderNBT(holderTag, stitcher);
            if (holder == null) {
                StellarCore.log.warn("[StellarCore-StitcherCache] Found null holder cache: `{}`, ignored.", holderTag.getString("sprite"));
                return;
            }
            holders.put(holder.getAtlasSprite().getIconName(), holder);
        });

        NBTTagList slotsTagList = tag.getTagList("slots", Constants.NBT.TAG_COMPOUND);
        // 提前填充 slot 列表，用于并行流。
        int bound = slotsTagList.tagCount();
        for (int i = 0; i < bound; i++) {
            this.slots.add(null);
        }
        IntStream.range(0, bound).parallel()
                .forEach(i -> slots.set(i, readSlotNBT(slotsTagList.getCompoundTagAt(i))));

        width = tag.getInteger("width");
        height = tag.getInteger("height");
    }

    private Stitcher.Holder readHolderNBT(NBTTagCompound tag, Stitcher stitcher) {
        TextureAtlasSprite sprite = cacheFor.getTextureExtry(tag.getString("sprite"));
        if (sprite == null) {
            if (tag.getBoolean("empty")) {
                sprite = cacheFor.getMissingSprite();
            } else {
                return null;
            }
        }

        AccessorStitcher stitcherAccessor = (AccessorStitcher) stitcher;
        Stitcher.Holder holder = new Stitcher.Holder(sprite, stitcherAccessor.getMipmapLevelStitcher());

        boolean rotated = tag.getBoolean("rotated");
        if (holder.isRotated() != rotated) {
            holder.rotate();
        }

        int maxTileDimension = stitcherAccessor.getMaxTileDimension();
        if (maxTileDimension > 0) {
            holder.setNewDimension(maxTileDimension);
        }

        return holder;
    }

    private Stitcher.Slot readSlotNBT(NBTTagCompound tag) {
        int originX = tag.getInteger("originX");
        int originY = tag.getInteger("originY");
        int width = tag.getInteger("width");
        int height = tag.getInteger("height");

        Stitcher.Slot slot = new Stitcher.Slot(originX, originY, width, height);
        AccessorStitcherSlot slotAccessor = (AccessorStitcherSlot) slot;

        NBTTagList subSlotsTag = tag.getTagList("subSlots", Constants.NBT.TAG_COMPOUND);
        if (subSlotsTag.tagCount() > 0) {
            List<Stitcher.Slot> subSlots = new ArrayList<>();
            int bound = subSlotsTag.tagCount();
            for (int i = 0; i < bound; i++) {
                NBTTagCompound tagAt = subSlotsTag.getCompoundTagAt(i);
                // Recursive
                Stitcher.Slot readSlotNBT = readSlotNBT(tagAt);
                subSlots.add(readSlotNBT);
            }
            slotAccessor.setSubSlots(subSlots);
        }
        if (!tag.getBoolean("holderEmpty")) {
            Stitcher.Holder holder = holders.get(tag.getString("holder"));
            slotAccessor.setHolder(holder);
        }

        return slot;
    }

    private static boolean holderEquals(Stitcher.Holder self, Stitcher.Holder another) {
        AccessorStitcherHolder selfAccessor = (AccessorStitcherHolder) self;
        AccessorStitcherHolder anotherHolderAccessor = (AccessorStitcherHolder) another;
        return selfAccessor.realWidth() == anotherHolderAccessor.realWidth() &&
               selfAccessor.realHeight() == anotherHolderAccessor.realHeight() &&
               self.isRotated() == another.isRotated() &&
               selfAccessor.scaleFactor() == anotherHolderAccessor.scaleFactor();
    }

    public enum State {
        UNKNOWN,
        TAG_READY,
        AVAILABLE,
        UNAVAILABLE,
    }

}
