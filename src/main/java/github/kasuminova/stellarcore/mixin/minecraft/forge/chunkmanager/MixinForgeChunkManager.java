package github.kasuminova.stellarcore.mixin.minecraft.forge.chunkmanager;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.ForgeChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Mixin(ForgeChunkManager.class)
public abstract class MixinForgeChunkManager {

    @Nonnull
    @Shadow(remap = false)
    @SuppressWarnings("DataFlowIssue")
    public static ImmutableSetMultimap<ChunkPos, ForgeChunkManager.Ticket> getPersistentChunksFor(final World world) {
        return null;
    }

    /**
     * @author Kasumi_Nova
     * @reason Performance optimization
     */
    @Overwrite(remap = false)
    public static Iterator<Chunk> getPersistentChunksIterableFor(final World world, final Iterator<Chunk> chunkIterator) {
        final ImmutableSetMultimap<ChunkPos, ForgeChunkManager.Ticket> persistentChunksFor = getPersistentChunksFor(world);
        final Set<ChunkPos> positions = persistentChunksFor.keySet();
        final List<Chunk> chunks = new ObjectArrayList<>(positions.size());
        final LongSet forcedPositions = new LongOpenHashSet(positions.size());
        final IChunkProvider chunkProvider = world.getChunkProvider();

        world.profiler.startSection("forcedChunkLoading");
        for (final ChunkPos pos : positions) {
            forcedPositions.add(ChunkPos.asLong(pos.x, pos.z));
            final Chunk loadedChunk = chunkProvider.getLoadedChunk(pos.x, pos.z);
            chunks.add(loadedChunk != null ? loadedChunk : chunkProvider.provideChunk(pos.x, pos.z));
        }
        world.profiler.endSection();

        return Iterators.concat(chunks.iterator(), Iterators.filter(chunkIterator,
            chunk -> !forcedPositions.contains(ChunkPos.asLong(chunk.x, chunk.z))));
    }

}
