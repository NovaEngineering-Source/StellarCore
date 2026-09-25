package github.kasuminova.stellarcore.client.model.vanillacache;

import net.minecraft.util.ResourceLocation;

public final class BlockstateSnapshot {

    public final ResourceLocation location;

    public final byte[][] parts;

    public BlockstateSnapshot(final ResourceLocation location, final byte[][] parts) {
        this.location = location;
        this.parts = parts;
    }

    public int partCount() {
        return parts.length;
    }
}
