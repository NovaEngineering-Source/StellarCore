package github.kasuminova.stellarcore.mixin.minecraft.forge.itemmodel;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraftforge.client.ItemModelMesherForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Retains the normal mutable maps, starting each item's metadata table at one entry. */
@Mixin(value = ItemModelMesherForge.class, priority = 1100)
public abstract class MixinItemModelMesherForge {

    // VintageFix can overwrite these methods at priority 1000. Apply after their
    // main merge, keeping its lazy model lookup and cache invalidation intact.
    // Its rebuildCache has no allocation, so that optional target needs no match.
    @Redirect(
        method = {"register", "rebuildCache"},
        at = @At(value = "NEW", target = "()Lit/unimi/dsi/fastutil/ints/Int2ObjectOpenHashMap;", remap = false),
        require = 0
    )
    private <V> Int2ObjectOpenHashMap<V> stellar_core$createMetadataMap() {
        return new Int2ObjectOpenHashMap<>(1);
    }
}
