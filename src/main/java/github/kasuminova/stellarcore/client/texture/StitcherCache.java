package github.kasuminova.stellarcore.client.texture;


import github.kasuminova.stellarcore.common.util.LargeNBTUtils;
import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.minecraft.stitcher.AccessorStitcher;
import github.kasuminova.stellarcore.mixin.util.AccessorStitcherHolder;
import github.kasuminova.stellarcore.mixin.util.AccessorStitcherSlot;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

    /** The compact layout parsed from disk; null while the file is absent, broken or still in the legacy NBT form. */
    private StitcherCacheFile data = null;

    /** The legacy NBT form, still readable so a cache written before the format change stays usable. */
    private NBTTagCompound readTag = null;

    private volatile Set<String> cachedSpriteNamesFromFile = null;

    private List<Stitcher.Holder> extraHolders = null;

    private int width = 0;
    private int height = 0;

    private State cacheState = State.UNKNOWN;

    private StitcherCache(final String name, final TextureMap cacheFor) {
        this.name = name;
        this.cacheFile = new File(CACHE_FILE_NAME.replace("{}", name));
        this.cacheFor = cacheFor;
        this.readTask = CompletableFuture.runAsync(this::readFromFile);
        StellarLog.LOG.info("[StellarCore-StitcherCache] Created stitcher cache for `{}`.", cacheFor.getBasePath());
        StellarLog.LOG.info("[StellarCore-StitcherCache] Stitcher file cache `{}` reader task started.", cacheFor.getBasePath());
    }

    public static StitcherCache create(final String name, final TextureMap cacheFor) {
        StitcherCache cache = CREATED_STITCHER_CACHE.get(cacheFor);
        if (cache != null) {
            cache.checkReadTaskState();
            cache.readTask = CompletableFuture.runAsync(cache::readFromFile);
            StellarLog.LOG.info("[StellarCore-StitcherCache] Stitcher file cache `{}` reader task restarted.", cacheFor.getBasePath());
            return cache;
        }
        cache = new StitcherCache(name, cacheFor);
        CREATED_STITCHER_CACHE.put(cacheFor, cache);
        return cache;
    }

    public static boolean hasCacheFor(final TextureMap textureMap) {
        return CREATED_STITCHER_CACHE.containsKey(textureMap);
    }

    public static StitcherCache getCacheFor(final TextureMap textureMap) {
        return CREATED_STITCHER_CACHE.get(textureMap);
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

    /**
     * Writes the cached layout through a temporary file, so a crash or a full disk can never leave a partially
     * written cache behind: the previous file either stays intact or is replaced by a complete one.
     */
    public void writeToFile() {
        final File temporary = new File(this.cacheFile.getParentFile(), this.cacheFile.getName() + ".tmp");
        try {
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                    new GZIPOutputStream(new FileOutputStream(temporary))))) {
                out.writeInt(StitcherCacheFile.MAGIC);
                toCacheFile().writeTo(out);
            }
            Files.move(temporary.toPath(), this.cacheFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            StellarLog.LOG.info("[StellarCore-StitcherCache] Successfully write stitcher cache file to `{}`.", cacheFile.getAbsolutePath());
        } catch (Throwable e) {
            StellarLog.LOG.error("[StellarCore-StitcherCache] Failed to write stitcher cache file! Please report it.", e);
            if (temporary.exists() && !temporary.delete()) {
                StellarLog.LOG.warn("[StellarCore-StitcherCache] Could not remove temporary cache file `{}`.", temporary.getAbsolutePath());
            }
        }
    }

    private StitcherCacheFile toCacheFile() {
        final List<StitcherCacheFile.HolderEntry> holderEntries = new ArrayList<>(this.holders.size());
        final Object2IntMap<String> holderIndexes = new Object2IntOpenHashMap<>(this.holders.size());
        holderIndexes.defaultReturnValue(-1);
        for (final Stitcher.Holder holder : this.holders.values()) {
            final AccessorStitcherHolder accessor = (AccessorStitcherHolder) holder;
            final String sprite = holder.getAtlasSprite().getIconName();
            holderIndexes.put(sprite, holderEntries.size());
            // Dimensions and scale travel with the entry: they are what lets a later run reject a layout whose
            // sprites changed size, which the name-only NBT form could not see.
            holderEntries.add(new StitcherCacheFile.HolderEntry(sprite, accessor.realWidth(), accessor.realHeight(),
                accessor.scaleFactor(), holder.isRotated(), holder.getAtlasSprite() == cacheFor.getMissingSprite()));
        }
        final List<StitcherCacheFile.SlotEntry> slotEntries = new ArrayList<>(this.slots.size());
        for (final Stitcher.Slot slot : this.slots) {
            slotEntries.add(toSlotEntry(slot, holderIndexes));
        }
        return StitcherCacheFile.of(holderEntries, slotEntries, this.width, this.height);
    }

    private static StitcherCacheFile.SlotEntry toSlotEntry(final Stitcher.Slot slot,
                                                           final Object2IntMap<String> holderIndexes) {
        final AccessorStitcherSlot accessor = (AccessorStitcherSlot) slot;
        final Stitcher.Holder holder = slot.getStitchHolder();
        final int holderIndex;
        if (holder == null) {
            holderIndex = StitcherCacheFile.noHolder();
        } else {
            var i = holderIndexes.getInt(holder.getAtlasSprite().getIconName());
            holderIndex = i == -1 ? StitcherCacheFile.noHolder() : i;
        }
        final List<Stitcher.Slot> subSlots = accessor.subSlots();
        final List<StitcherCacheFile.SlotEntry> subEntries =
            subSlots == null || subSlots.isEmpty() ? Collections.emptyList() : new ArrayList<>(subSlots.size());
        if (subSlots != null) {
            for (final Stitcher.Slot subSlot : subSlots) {
                subEntries.add(toSlotEntry(subSlot, holderIndexes));
            }
        }
        return new StitcherCacheFile.SlotEntry(slot.getOriginX(), slot.getOriginY(), accessor.width(),
            accessor.height(), holderIndex, subEntries);
    }

    public void readFromFile() {
        if (!cacheFile.exists()) {
            this.cacheState = State.UNAVAILABLE;
            StellarLog.LOG.info("[StellarCore-StitcherCache] Stitcher cache file is unavailable (File not found).");
            return;
        }

        try {
            if (isCompactCache()) {
                try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(cacheFile)))) {
                    in.readInt();
                    this.data = StitcherCacheFile.readFrom(in);
                }
            } else {
                try (FileInputStream fis = new FileInputStream(cacheFile)) {
                    this.readTag = LargeNBTUtils.readCompressed(fis);
                }
            }
            this.cacheState = State.TAG_READY;
            this.cachedSpriteNamesFromFile = null;
            StellarLog.LOG.info("[StellarCore-StitcherCache] Successfully read stitcher cache file from `{}`.", cacheFile.getAbsolutePath());
        } catch (Throwable e) {
            this.data = null;
            this.readTag = null;
            this.cacheState = State.UNAVAILABLE;
            StellarLog.LOG.warn("[StellarCore-StitcherCache] Failed to read stitcher cache file, it may be broken.", e);
            // A cache that cannot be read is of no use, and keeping it only makes the next launch fail the same
            // way; the layout is recomputed and written again.
            if (!cacheFile.delete()) {
                StellarLog.LOG.warn("[StellarCore-StitcherCache] Could not remove broken cache file `{}`.", cacheFile.getAbsolutePath());
            }
        }
    }

    private boolean isCompactCache() throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(cacheFile)))) {
            return in.readInt() == StitcherCacheFile.MAGIC;
        }
    }

    /**
     * Best-effort: returns sprite names from the on-disk cache file tag (if ready).
     *
     * <p>Used to stabilize stitching inputs across runs when mods register sprites nondeterministically.
     */
    public Set<String> getCachedSpriteNamesFromFile() {
        checkReadTaskState();

        if (cacheState != State.TAG_READY || readTag == null) {
            return Collections.emptySet();
        }

        Set<String> cached = this.cachedSpriteNamesFromFile;
        if (cached != null) {
            return cached;
        }

        if (this.data != null) {
            final List<StitcherCacheFile.HolderEntry> entries = this.data.holders();
            final ObjectOpenHashSet<String> spriteNames = new ObjectOpenHashSet<>(entries.size());
            for (final StitcherCacheFile.HolderEntry entry : entries) {
                spriteNames.add(entry.sprite());
            }
            this.cachedSpriteNamesFromFile = spriteNames;
            return spriteNames;
        }

        NBTTagList holdersTagList = readTag.getTagList("holders", Constants.NBT.TAG_COMPOUND);
        ObjectOpenHashSet<String> spriteNames = new ObjectOpenHashSet<>(holdersTagList.tagCount());
        for (int i = 0; i < holdersTagList.tagCount(); i++) {
            NBTTagCompound holderTag = holdersTagList.getCompoundTagAt(i);
            spriteNames.add(holderTag.getString("sprite"));
        }

        this.cachedSpriteNamesFromFile = spriteNames;
        return spriteNames;
    }

    public void parseTag(final Stitcher stitcher, final Set<Stitcher.Holder> targetHolders) {
        checkReadTaskState();

        if (cacheState != State.TAG_READY) {
            return;
        }

        try {
            if (this.data != null) {
                parseData(stitcher, targetHolders);
                return;
            }
            fromNBT(readTag, stitcher);
            this.cacheState = holdersEquals(targetHolders) ? State.AVAILABLE : State.UNAVAILABLE;
            StellarLog.LOG.info("[StellarCore-StitcherCache] Stitcher cache parsed, state: {}.", this.cacheState);
        } catch (Throwable e) {
            StellarLog.LOG.warn("[StellarCore-StitcherCache] Failed to parse stitcher cache file, it may be broken.", e);
        } finally {
            // The file form is fully consumed here: the layout lives in the holders and slots from now on, and the
            // sprite names were only needed while sprites were being registered. Holding either for the rest of the
            // session would keep a second copy of the atlas layout in memory for nothing.
            this.data = null;
            this.readTag = null;
            this.cachedSpriteNamesFromFile = null;
        }
    }

    /**
     * Validates the compact layout against this run's sprites before building any of it.
     *
     * <p>The name-only form had to construct every holder and slot before it could tell whether the layout was
     * usable, so a stale cache paid for a tree it then threw away. The compact form carries the dimensions and the
     * scale each holder had when it was written, which is enough to reject a stale layout up front and to build the
     * tree only for a layout that is actually reused.</p>
     */
    private void parseData(final Stitcher stitcher, final Set<Stitcher.Holder> targetHolders) {
        this.extraHolders = null;

        final List<StitcherCacheFile.HolderEntry> entries = this.data.holders();
        final Map<String, StitcherCacheFile.HolderEntry> cached = new Object2ObjectOpenHashMap<>(entries.size());
        int resolvable = 0;
        for (final StitcherCacheFile.HolderEntry entry : entries) {
            cached.put(entry.sprite(), entry);
            if (spriteFor(entry) != null) {
                resolvable++;
            }
        }

        final List<Stitcher.Holder> extras = new ArrayList<>();
        int matched = 0;
        for (final Stitcher.Holder target : targetHolders) {
            final String spriteName = target.getAtlasSprite().getIconName();
            final StitcherCacheFile.HolderEntry entry = cached.get(spriteName);
            if (entry == null) {
                // Runtime has a sprite that the cache doesn't — record as extra.
                extras.add(target);
                continue;
            }
            final AccessorStitcherHolder accessor = (AccessorStitcherHolder) target;
            if (accessor.realWidth() != entry.width() || accessor.realHeight() != entry.height()
                || accessor.scaleFactor() != entry.scale() || target.isRotated() != entry.rotated()) {
                StellarLog.LOG.warn("[StellarCore-StitcherCache] Stitcher cache is unavailable, holder `{}` changed "
                        + "(cached {}x{} scale {}, now {}x{} scale {}).", spriteName,
                    entry.width(), entry.height(), entry.scale(),
                    accessor.realWidth(), accessor.realHeight(), accessor.scaleFactor());
                this.cacheState = State.UNAVAILABLE;
                return;
            }
            matched++;
        }

        if (matched != resolvable) {
            // Cache has sprites that runtime doesn't — cache is stale.
            StellarLog.LOG.warn("[StellarCore-StitcherCache] Stitcher cache is unavailable, {} cached holders not found in runtime.", resolvable - matched);
            this.cacheState = State.UNAVAILABLE;
            return;
        }

        if (!extras.isEmpty()) {
            // Runtime is a strict superset of cache — partial match.
            // These extra holders will be allocated into the cached layout.
            this.extraHolders = extras;
            StellarLog.LOG.info("[StellarCore-StitcherCache] Cache is a partial match: {} extra sprites in runtime (nondeterministic registration); will allocate incrementally.", extras.size());
        }

        fromData(stitcher);
        this.cacheState = State.AVAILABLE;
        StellarLog.LOG.info("[StellarCore-StitcherCache] Stitcher cache parsed, state: {}.", this.cacheState);
    }

    private void fromData(final Stitcher stitcher) {
        this.holders.clear();
        this.slots.clear();

        // Positions are kept even for entries whose sprite is gone, because the slots address holders by position.
        final List<StitcherCacheFile.HolderEntry> entries = this.data.holders();
        final List<Stitcher.Holder> materialised = new ArrayList<>(entries.size());
        for (final StitcherCacheFile.HolderEntry entry : entries) {
            final Stitcher.Holder holder = materialiseHolder(stitcher, entry);
            materialised.add(holder);
            if (holder != null) {
                this.holders.put(holder.getAtlasSprite().getIconName(), holder);
            }
        }

        final List<StitcherCacheFile.SlotEntry> slotEntries = this.data.slots();
        for (final StitcherCacheFile.SlotEntry slotEntry : slotEntries) {
            this.slots.add(materialiseSlot(slotEntry, materialised));
        }

        this.width = this.data.width();
        this.height = this.data.height();
    }

    private Stitcher.Holder materialiseHolder(final Stitcher stitcher, final StitcherCacheFile.HolderEntry entry) {
        final TextureAtlasSprite sprite = spriteFor(entry);
        if (sprite == null) {
            StellarLog.LOG.warn("[StellarCore-StitcherCache] Found null holder cache: `{}`, ignored.", entry.sprite());
            return null;
        }

        AccessorStitcher stitcherAccessor = (AccessorStitcher) stitcher;
        Stitcher.Holder holder = new Stitcher.Holder(sprite, stitcherAccessor.getMipmapLevelStitcher());

        if (holder.isRotated() != entry.rotated()) {
            holder.rotate();
        }

        int maxTileDimension = stitcherAccessor.getMaxTileDimension();
        if (maxTileDimension > 0) {
            holder.setNewDimension(maxTileDimension);
        }

        return holder;
    }

    private TextureAtlasSprite spriteFor(final StitcherCacheFile.HolderEntry entry) {
        final TextureAtlasSprite sprite = cacheFor.getTextureExtry(entry.sprite());
        if (sprite != null) {
            return sprite;
        }
        return entry.empty() ? cacheFor.getMissingSprite() : null;
    }

    private Stitcher.Slot materialiseSlot(final StitcherCacheFile.SlotEntry entry,
                                          final List<Stitcher.Holder> holders) {
        final Stitcher.Slot slot = new Stitcher.Slot(entry.originX(), entry.originY(), entry.width(), entry.height());
        final AccessorStitcherSlot slotAccessor = (AccessorStitcherSlot) slot;

        if (!entry.subSlots().isEmpty()) {
            final List<Stitcher.Slot> subSlots = new ArrayList<>(entry.subSlots().size());
            for (final StitcherCacheFile.SlotEntry subEntry : entry.subSlots()) {
                subSlots.add(materialiseSlot(subEntry, holders));
            }
            slotAccessor.setSubSlots(subSlots);
        }

        final int holderIndex = entry.holder();
        if (holderIndex >= 0 && holderIndex < holders.size()) {
            slotAccessor.setHolder(holders.get(holderIndex));
        }

        return slot;
    }

    /**
     * Checks the cached layout against the sprites this run is about to stitch.
     *
     * <p>Both sides are keyed by sprite name and a texture map registers each name once, so counting matches is
     * enough to tell "the cache has sprites this run does not" from "this run has sprites the cache does not":
     * only the latter is usable, and its extra sprites are recorded for incremental allocation. The map is read
     * directly rather than copied first, which used to allocate an entry per registered sprite on every reload.</p>
     *
     * @param targetHolders sprites this run will stitch
     * @return whether the cached layout can be reused
     */
    public boolean holdersEquals(Set<Stitcher.Holder> targetHolders) {
        this.extraHolders = null;

        List<Stitcher.Holder> extras = new ArrayList<>();
        int matched = 0;

        for (final Stitcher.Holder target : targetHolders) {
            String spriteName = target.getAtlasSprite().getIconName();
            Stitcher.Holder cached = this.holders.get(spriteName);
            if (cached == null) {
                // Runtime has a sprite that the cache doesn't — record as extra.
                extras.add(target);
                continue;
            }
            if (!holderEquals(cached, target)) {
                StellarLog.LOG.warn("[StellarCore-StitcherCache] Stitcher cache is unavailable, holder `{}` not equals.", spriteName);
                return false;
            }
            matched++;
        }

        if (matched != this.holders.size()) {
            // Cache has sprites that runtime doesn't — cache is stale.
            StellarLog.LOG.warn("[StellarCore-StitcherCache] Stitcher cache is unavailable, {} cached holders not found in runtime.", this.holders.size() - matched);
            return false;
        }

        if (!extras.isEmpty()) {
            // Runtime is a strict superset of cache — partial match.
            // These extra holders will be allocated into the cached layout.
            this.extraHolders = extras;
            StellarLog.LOG.info("[StellarCore-StitcherCache] Cache is a partial match: {} extra sprites in runtime (nondeterministic registration); will allocate incrementally.", extras.size());
        }

        return true;
    }

    public List<Stitcher.Holder> getExtraHolders() {
        return extraHolders;
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
    
    public void clear() {
        this.holders.clear();
        this.slots.clear();
        this.data = null;
        this.readTag = null;
        this.cachedSpriteNamesFromFile = null;
        this.extraHolders = null;
        this.width = 0;
        this.height = 0;
        this.cacheState = State.UNKNOWN;
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

    private void checkReadTaskState() {
        if (readTask != null) {
            if (!readTask.isDone()) {
                try {
                    readTask.get();
                } catch (Throwable e) {
                    StellarLog.LOG.error("[StellarCore-StitcherCache] Failed to read stitcher cache file! Please report it.", e);
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
                StellarLog.LOG.warn("[StellarCore-StitcherCache] Found null holder cache: `{}`, ignored.", holderTag.getString("sprite"));
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
        if (selfAccessor.realWidth() == anotherHolderAccessor.realWidth()) {
            if (selfAccessor.realHeight() == anotherHolderAccessor.realHeight()) {
                if (self.isRotated() == another.isRotated()) {
                    if (selfAccessor.scaleFactor() == anotherHolderAccessor.scaleFactor()) {
                        return true;
                    } else {
                        StellarLog.LOG.warn("[StellarCore-StitcherCache] Holder `{}` and `{}` are not equal (ScaleFactor {} ≠ {}).",
                                self.getAtlasSprite().getIconName(), another.getAtlasSprite().getIconName(),
                                selfAccessor.scaleFactor(), anotherHolderAccessor.scaleFactor()
                        );
                    }
                } else {
                    StellarLog.LOG.warn("[StellarCore-StitcherCache] Holder `{}` and `{}` are not equal (Rotated {} ≠ {}).",
                            self.getAtlasSprite().getIconName(), another.getAtlasSprite().getIconName(),
                            self.isRotated(), another.isRotated()
                    );
                }
            } else {
                StellarLog.LOG.warn("[StellarCore-StitcherCache] Holder `{}` and `{}` are not equal (Height {} ≠ {}).",
                        self.getAtlasSprite().getIconName(), another.getAtlasSprite().getIconName(),
                        selfAccessor.realHeight(), anotherHolderAccessor.realHeight()
                );
            }
        } else {
            StellarLog.LOG.warn("[StellarCore-StitcherCache] Holder `{}` and `{}` are not equal (Width {} ≠ {}).",
                    self.getAtlasSprite().getIconName(), another.getAtlasSprite().getIconName(),
                    selfAccessor.realWidth(), anotherHolderAccessor.realWidth()
            );
        }
        return false;
    }

    public enum State {
        UNKNOWN,
        TAG_READY,
        AVAILABLE,
        UNAVAILABLE,
    }

}
