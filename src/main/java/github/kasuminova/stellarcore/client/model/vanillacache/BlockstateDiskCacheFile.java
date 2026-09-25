package github.kasuminova.stellarcore.client.model.vanillacache;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Map;

public final class BlockstateDiskCacheFile {

    public static final int MAGIC = 0x53434253;   // "SCBS"
    public static final int VERSION = 1;

    private static final int MAX_ENTRIES = 1 << 20;
    private static final int MAX_PARTS = 1 << 10;
    private static final int MAX_JSON_BYTES = 32 << 20;

    private BlockstateDiskCacheFile() {
    }

    public static void write(final DataOutputStream out,
                             final long seed,
                             final Map<ResourceLocation, BlockstateSnapshot> entries) throws IOException {
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        out.writeLong(seed);
        out.writeInt(entries.size());
        for (final BlockstateSnapshot snapshot : entries.values()) {
            out.writeUTF(snapshot.location.toString());
            out.writeInt(snapshot.parts.length);
            for (final byte[] part : snapshot.parts) {
                out.writeInt(part.length);
                out.write(part);
            }
        }
        out.flush();
    }

    public static final class ReadResult {
        public final long seed;
        public final Map<ResourceLocation, BlockstateSnapshot> entries;

        ReadResult(final long seed, final Map<ResourceLocation, BlockstateSnapshot> entries) {
            this.seed = seed;
            this.entries = entries;
        }
    }

    @Nullable
    public static ReadResult read(final DataInputStream in, final long expectedSeed) throws IOException {
        if (in.readInt() != MAGIC) {
            return null;
        }
        if (in.readInt() != VERSION) {
            return null;
        }
        final long seed = in.readLong();
        if (seed != expectedSeed) {
            return null;
        }

        final int count = in.readInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IOException("unreasonable blockstate entry count: " + count);
        }

        final Map<ResourceLocation, BlockstateSnapshot> entries =
                new Object2ObjectOpenHashMap<>(Math.max(16, count * 2));
        for (int i = 0; i < count; i++) {
            final String locationText = in.readUTF();
            final int partCount = in.readInt();
            if (partCount <= 0 || partCount > MAX_PARTS) {
                throw new IOException("unreasonable blockstate part count at entry " + i + ": " + partCount);
            }
            final byte[][] parts = new byte[partCount][];
            for (int part = 0; part < partCount; part++) {
                final int length = in.readInt();
                if (length <= 0 || length > MAX_JSON_BYTES) {
                    throw new IOException("unreasonable blockstate part length at entry " + i + ": " + length);
                }
                parts[part] = new byte[length];
                in.readFully(parts[part]);
            }

            final ResourceLocation location;
            try {
                location = new ResourceLocation(locationText);
            } catch (Throwable badLocation) {
                throw new IOException("bad blockstate location at entry " + i + ": " + locationText, badLocation);
            }

            entries.put(location, new BlockstateSnapshot(location, parts));
        }

        try {
            in.readByte();
            throw new IOException("trailing data after blockstate cache");
        } catch (EOFException expected) {
            // Clean end of stream.
        }
        return new ReadResult(seed, entries);
    }
}
