package github.kasuminova.stellarcore.client.texture;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class StitcherCacheFile {

    static final int MAGIC = 0x53435331;

    static final int VERSION = 1;

    private static final int FLAG_ROTATED = 1;
    private static final int FLAG_EMPTY = 2;

    private static final int NO_HOLDER = -1;

    private final List<HolderEntry> holders;
    private final List<SlotEntry> slots;
    private final int width;
    private final int height;

    private StitcherCacheFile(final List<HolderEntry> holders, final List<SlotEntry> slots, final int width,
                              final int height) {
        this.holders = holders;
        this.slots = slots;
        this.width = width;
        this.height = height;
    }

    static StitcherCacheFile of(final List<HolderEntry> holders, final List<SlotEntry> slots, final int width,
                                final int height) {
        return new StitcherCacheFile(holders, slots, width, height);
    }

    List<HolderEntry> holders() {
        return this.holders;
    }

    List<SlotEntry> slots() {
        return this.slots;
    }

    int width() {
        return this.width;
    }

    int height() {
        return this.height;
    }

    void writeTo(final DataOutputStream out) throws IOException {
        out.writeInt(VERSION);
        out.writeInt(this.width);
        out.writeInt(this.height);
        out.writeInt(this.holders.size());
        out.writeInt(this.slots.size());
        for (final HolderEntry holder : this.holders) {
            out.writeUTF(holder.sprite());
            out.writeInt(holder.width());
            out.writeInt(holder.height());
            out.writeFloat(holder.scale());
            out.writeByte((holder.rotated() ? FLAG_ROTATED : 0) | (holder.empty() ? FLAG_EMPTY : 0));
        }
        for (final SlotEntry slot : this.slots) {
            writeSlot(out, slot);
        }
    }

    static StitcherCacheFile readFrom(final DataInputStream in) throws IOException {
        final int version = in.readInt();
        if (version != VERSION) {
            throw new IOException("Unsupported stitcher cache version " + version);
        }
        final int width = in.readInt();
        final int height = in.readInt();
        final int holderCount = in.readInt();
        final int slotCount = in.readInt();
        if (holderCount < 0 || slotCount < 0) {
            throw new IOException("Negative stitcher cache entry count");
        }

        final List<HolderEntry> holders = new ArrayList<>(holderCount);
        for (int i = 0; i < holderCount; i++) {
            final String sprite = in.readUTF();
            final int holderWidth = in.readInt();
            final int holderHeight = in.readInt();
            final float scale = in.readFloat();
            final int flags = in.readByte();
            holders.add(new HolderEntry(sprite, holderWidth, holderHeight, scale,
                (flags & FLAG_ROTATED) != 0, (flags & FLAG_EMPTY) != 0));
        }

        final List<SlotEntry> slots = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            slots.add(readSlot(in));
        }
        return new StitcherCacheFile(holders, slots, width, height);
    }

    private static void writeSlot(final DataOutputStream out, final SlotEntry slot) throws IOException {
        out.writeInt(slot.originX());
        out.writeInt(slot.originY());
        out.writeInt(slot.width());
        out.writeInt(slot.height());
        out.writeInt(slot.holder());
        out.writeInt(slot.subSlots().size());
        for (final SlotEntry subSlot : slot.subSlots()) {
            writeSlot(out, subSlot);
        }
    }

    private static SlotEntry readSlot(final DataInputStream in) throws IOException {
        final int originX = in.readInt();
        final int originY = in.readInt();
        final int width = in.readInt();
        final int height = in.readInt();
        final int holder = in.readInt();
        final int subSlotCount = in.readInt();
        if (subSlotCount < 0) {
            throw new IOException("Negative sub-slot count");
        }
        final List<SlotEntry> subSlots = new ArrayList<>(subSlotCount);
        for (int i = 0; i < subSlotCount; i++) {
            subSlots.add(readSlot(in));
        }
        return new SlotEntry(originX, originY, width, height, holder, subSlots);
    }

    static int noHolder() {
        return NO_HOLDER;
    }

    static final class HolderEntry {

        private final String sprite;
        private final int width;
        private final int height;
        private final float scale;
        private final boolean rotated;
        private final boolean empty;

        HolderEntry(final String sprite, final int width, final int height, final float scale, final boolean rotated,
                    final boolean empty) {
            this.sprite = sprite;
            this.width = width;
            this.height = height;
            this.scale = scale;
            this.rotated = rotated;
            this.empty = empty;
        }

        String sprite() {
            return this.sprite;
        }

        int width() {
            return this.width;
        }

        int height() {
            return this.height;
        }

        float scale() {
            return this.scale;
        }

        boolean rotated() {
            return this.rotated;
        }

        boolean empty() {
            return this.empty;
        }

    }

    static final class SlotEntry {

        private final int originX;
        private final int originY;
        private final int width;
        private final int height;
        private final int holder;
        private final List<SlotEntry> subSlots;

        SlotEntry(final int originX, final int originY, final int width, final int height, final int holder,
                  final List<SlotEntry> subSlots) {
            this.originX = originX;
            this.originY = originY;
            this.width = width;
            this.height = height;
            this.holder = holder;
            this.subSlots = subSlots;
        }

        int originX() {
            return this.originX;
        }

        int originY() {
            return this.originY;
        }

        int width() {
            return this.width;
        }

        int height() {
            return this.height;
        }

        int holder() {
            return this.holder;
        }

        List<SlotEntry> subSlots() {
            return this.subSlots;
        }

    }

}
