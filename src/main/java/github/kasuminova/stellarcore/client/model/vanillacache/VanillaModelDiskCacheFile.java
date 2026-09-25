package github.kasuminova.stellarcore.client.model.vanillacache;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Map;

/**
 * On-disk representation of the vanilla model cache.
 *
 * <p>Layout (big-endian; the caller wraps the stream in GZIP):</p>
 * <pre>
 *   int    MAGIC                 0x53434D43 ("SCMC")
 *   int    VERSION               see {@link #VERSION}
 *   long   SEED                  environment fingerprint, see {@link EnvironmentFingerprint}
 *   int    entryCount
 *   entryCount times:
 *     UTF    location            "namespace:path", *without* the "models/" prefix
 *     int    modelJsonLength
 *     byte[] modelJson           raw UTF-8 JSON
 *     bool   hasArmature
 *     [if hasArmature:]
 *       int  armatureJsonLength
 *       byte[] armatureJson      raw UTF-8 JSON
 * </pre>
 *
 * <p>There are deliberately no per-entry hashes. The environment fingerprint in
 * the file name plus the seed field is the whole validity contract: if any mod or
 * resource pack changed, a different file name is computed and this one is never
 * read. Re-reading and hashing every model on load would cost as much as not
 * caching at all, which is the mistake this format exists to avoid.</p>
 */
public final class VanillaModelDiskCacheFile {

    public static final int MAGIC = 0x53434D43;   // "SCMC"

    /** v1/v2 carried per-entry SHA-256 hashes; v3 trusts the environment fingerprint instead. */
    public static final int VERSION = 3;

    /** Guard against a corrupt or hostile file causing a huge allocation. */
    private static final int MAX_ENTRIES = 1 << 20;
    private static final int MAX_JSON_BYTES = 32 << 20;

    private VanillaModelDiskCacheFile() {
    }

    public static void write(final DataOutputStream out,
                             final long seed,
                             final Map<ResourceLocation, VanillaModelSnapshot> entries) throws IOException {
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        out.writeLong(seed);
        out.writeInt(entries.size());
        for (final VanillaModelSnapshot snapshot : entries.values()) {
            out.writeUTF(snapshot.location.toString());
            out.writeInt(snapshot.modelJson.length);
            out.write(snapshot.modelJson);
            if (snapshot.armatureJson != null) {
                out.writeBoolean(true);
                out.writeInt(snapshot.armatureJson.length);
                out.write(snapshot.armatureJson);
            } else {
                out.writeBoolean(false);
            }
        }
        out.flush();
    }

    /** Successful read result: the stored seed plus the decoded snapshots. */
    public static final class ReadResult {
        public final long seed;
        public final Map<ResourceLocation, VanillaModelSnapshot> entries;

        ReadResult(final long seed, final Map<ResourceLocation, VanillaModelSnapshot> entries) {
            this.seed = seed;
            this.entries = entries;
        }
    }

    /**
     * Read and validate a cache file.
     *
     * @return the decoded contents, or {@code null} when the file is not a
     *         usable cache (wrong magic, wrong version, or a seed that does not
     *         match the environment we are running in). Callers treat
     *         {@code null} as "no cache" and regenerate.
     * @throws IOException when the file is structurally corrupt
     */
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
            throw new IOException("unreasonable entry count: " + count);
        }

        final Map<ResourceLocation, VanillaModelSnapshot> entries =
                new Object2ObjectOpenHashMap<>(Math.max(16, count * 2));
        for (int i = 0; i < count; i++) {
            final String locationText = in.readUTF();

            final byte[] modelJson = readBlob(in, "model", i);
            final byte[] armatureJson = in.readBoolean() ? readBlob(in, "armature", i) : null;

            final ResourceLocation location;
            try {
                location = new ResourceLocation(locationText);
            } catch (Throwable badLocation) {
                throw new IOException("bad location at entry " + i + ": " + locationText, badLocation);
            }

            entries.put(location, new VanillaModelSnapshot(location, modelJson, armatureJson));
        }

        try {
            in.readByte();
            throw new IOException("trailing data after vanilla model cache");
        } catch (EOFException expected) {
            // Clean end of stream: exactly what we want.
        }
        return new ReadResult(seed, entries);
    }

    private static byte[] readBlob(final DataInputStream in,
                                   final String what,
                                   final int index) throws IOException {
        final int length = in.readInt();
        if (length <= 0 || length > MAX_JSON_BYTES) {
            throw new IOException("unreasonable " + what + " length at entry " + index + ": " + length);
        }
        final byte[] blob = new byte[length];
        in.readFully(blob);
        return blob;
    }
}
