package github.kasuminova.stellarcore.mixin.minecraft.forge.chunkmanager;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(PlayerChunkMap.class)
public class MixinPlayerChunkMap {

    @Shadow
    @Final
    private List<PlayerChunkMapEntry> entries;

    @Inject(method = "getChunkIterator", at = @At("HEAD"), cancellable = true)
    private void injectGetChunkIterator(final CallbackInfoReturnable<Iterator<Chunk>> cir) {
        final List<Chunk> validChunks = (this.entries.size() > 500 ? this.entries.parallelStream() : this.entries.stream())
                .filter(entry -> {
                    final Chunk chunk = entry.getChunk();

                    if (chunk == null) {
                        return false;
                    }
                    if (!chunk.isLightPopulated() && chunk.isTerrainPopulated()) {
                        return true;
                    }
                    if (!chunk.wasTicked()) {
                        return true;
                    }

                    return entry.hasPlayerMatchingInRange(128.0D, input -> input != null && !input.isSpectator());
                })
                .map(PlayerChunkMapEntry::getChunk)
                .collect(Collectors.toCollection(ObjectArrayList::new));
        cir.setReturnValue(validChunks.iterator());
    }

}
