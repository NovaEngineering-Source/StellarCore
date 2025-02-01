package github.kasuminova.stellarcore.mixin.minecraft.forge.chunkmanager;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterators;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashSet;
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
        final Set<Chunk> chunks = new NonBlockingHashSet<>();
        final List<ChunkPos> requiredToLoad = new ObjectArrayList<>();
        final IChunkProvider chunkProvider = world.getChunkProvider();

        // Use parallel stream to load all persistent chunks.
        world.profiler.endStartSection("regularChunkLoading");
        persistentChunksFor.keys().parallelStream().forEach(pos -> {
            Chunk loadedChunk = chunkProvider.getLoadedChunk(pos.x, pos.z);
            if (loadedChunk != null) {
                // Chunk is already loaded.
                chunks.add(loadedChunk);
            } else {
                synchronized (requiredToLoad) {
                    // Chunk is not loaded, queue to main thread for loading.
                    requiredToLoad.add(pos);
                }
            }
        });

        // Load all queued chunks.
        if (!requiredToLoad.isEmpty()) {
            world.profiler.endStartSection("forcedChunkLoading");
            requiredToLoad.forEach(pos -> chunks.add(chunkProvider.provideChunk(pos.x, pos.z)));
        }
        world.profiler.endSection();

        return Iterators.concat(chunks.iterator(), chunkIterator);
    }

}
