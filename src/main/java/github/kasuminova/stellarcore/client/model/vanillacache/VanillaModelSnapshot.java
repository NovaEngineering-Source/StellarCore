package github.kasuminova.stellarcore.client.model.vanillacache;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

/**
 * A single disk-cached vanilla JSON model entry.
 *
 * <p>Stores the raw UTF-8 JSON of the model and, when the model has one, its
 * companion armature (animation) JSON. Validation is intentionally delegated to
 * {@link EnvironmentFingerprint}: the mod list and ordered resource pack list are
 * fingerprinted up front, so a snapshot that made it into the in-memory view is
 * trusted without re-reading or hashing its source bytes. This is what makes the
 * cache hit path IO-free.</p>
 *
 * <p>Both halves are held as bytes rather than parsed objects so a snapshot that
 * is never requested costs nothing beyond its byte arrays, and so the cache file
 * format stays independent of Forge's {@code ModelBlock} class shape.</p>
 */
public final class VanillaModelSnapshot {

    /** Cache keys use the un-prefixed form (no leading {@code "models/"}). */
    public final ResourceLocation location;

    public final byte[] modelJson;

    /** {@code null} when this model has no companion armature file. */
    @Nullable
    public final byte[] armatureJson;

    public VanillaModelSnapshot(final ResourceLocation location,
                                final byte[] modelJson,
                                @Nullable final byte[] armatureJson) {
        this.location = location;
        this.modelJson = modelJson;
        this.armatureJson = armatureJson;
    }

    public String modelJsonText() {
        return new String(modelJson, StandardCharsets.UTF_8);
    }

    @Nullable
    public String armatureJsonText() {
        return armatureJson == null ? null : new String(armatureJson, StandardCharsets.UTF_8);
    }

    public boolean hasArmature() {
        return armatureJson != null;
    }
}
