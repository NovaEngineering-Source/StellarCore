package github.kasuminova.stellarcore.common.world;

import com.github.bsideup.jabel.Desugar;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class ParallelRandomBlockTicker {

    public static final ParallelRandomBlockTicker INSTANCE = new ParallelRandomBlockTicker();

    private final Map<Chunk, List<TickData>> enqueuedChunks = new Reference2ObjectOpenHashMap<>();
    private final Map<Chunk, List<RandomTickTask>> randomTickData = new ConcurrentHashMap<>();

    private World currentWorld = null;
    private Random currentRand = null;
    private Profiler profiler = null;

    private ParallelRandomBlockTicker() {
    }

    public void enqueueChunk(final Chunk chunk, final List<TickData> data) {
        enqueuedChunks.put(chunk, data);
    }

    public void execute(final World world, final Random rand, final Profiler profiler, final int randomTickSpeed) {
        if (enqueuedChunks.isEmpty()) {
            return;
        }

        this.currentWorld = world;
        this.currentRand = rand;
        this.profiler = profiler;

        if (this.enqueuedChunks.size() * randomTickSpeed >= 1000) {
            this.enqueuedChunks.entrySet().parallelStream().forEach(entry -> {
                Chunk chunk = entry.getKey();
                for (final TickData tickData : entry.getValue()) {
                    List<RandomTickTask> data = getRandomTickData(chunk, tickData);
                    if (data.isEmpty()) {
                        continue;
                    }
                    this.randomTickData.computeIfAbsent(chunk, (k) -> new ObjectArrayList<>()).addAll(data);
                }
            });
        } else {
            this.enqueuedChunks.forEach((chunk, value) -> {
                for (final TickData tickData : value) {
                    List<RandomTickTask> data = getRandomTickData(chunk, tickData);
                    if (data.isEmpty()) {
                        continue;
                    }
                    this.randomTickData.computeIfAbsent(chunk, (k) -> new ObjectArrayList<>()).addAll(data);
                }
            });
        }

        for (Chunk chunk : this.enqueuedChunks.keySet()) {
            List<RandomTickTask> data = this.randomTickData.get(chunk);
            if (data != null && !data.isEmpty()) {
                executeTask(data);
            }
        }

        this.enqueuedChunks.clear();
        this.randomTickData.clear();
    }

    private static List<RandomTickTask> getRandomTickData(Chunk chunk, TickData tickData) {
        ExtendedBlockStorage blockStorage = tickData.blockStorage;

        IntList lcgList = tickData.lcgList;
        List<RandomTickTask> enqueuedData = new ObjectArrayList<>(lcgList.size());
        IntListIterator it = lcgList.iterator();
        while (it.hasNext()) {
            int lcg = it.nextInt();
            int x = lcg & 15;
            int y = lcg >> 8 & 15;
            int z = lcg >> 16 & 15;
            IBlockState blockState = blockStorage.get(x, z, y);
            Block block = blockState.getBlock();

            if (block.getTickRandomly()) {
                int chunkXPos = chunk.x * 16;
                int chunkZPos = chunk.z * 16;
                BlockPos pos = new BlockPos(x + chunkXPos, y + blockStorage.getYLocation(), z + chunkZPos);
                enqueuedData.add(new RandomTickTask(blockStorage, pos, x, y, z));
            }
        }

        return enqueuedData;
    }

    private void executeTask(List<RandomTickTask> tickDataList) {
        Profiler profiler = this.profiler;
        profiler.startSection("randomTick");

        World world = currentWorld;
        Random rand = currentRand;
        for (final RandomTickTask tickData : tickDataList) {
            ExtendedBlockStorage storage = tickData.storage();
            IBlockState blockState = storage.get(tickData.storageX(), tickData.storageY(), tickData.storageZ());

            Block block = blockState.getBlock();
            if (block.getTickRandomly()) {
                block.randomTick(world, tickData.worldPos(), blockState, rand);
            }
        }

        profiler.endSection();
    }

    @Desugar
    public record TickData(ExtendedBlockStorage blockStorage, IntList lcgList) {
    }

    @Desugar
    public record RandomTickTask(ExtendedBlockStorage storage, BlockPos worldPos, int storageX, int storageY, int storageZ) {
    }

}
