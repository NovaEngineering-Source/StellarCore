package github.kasuminova.stellarcore.common.world;

import github.kasuminova.stellarcore.common.util.StellarEnvironment;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Collects the random tick candidates of one world tick into shared flat buffers, finds the candidates that really tick
 * randomly on the worker threads, and runs the random ticks themselves on the server thread.
 *
 * <p>The chunks of a tick used to be turned into one object list per section plus one queue entry per chunk, which for
 * a base with thousands of loaded chunks is a dozen small allocations per chunk every tick. The batch now lives in four
 * primitive arrays that are reused across ticks, and each worker gets an index range of those arrays instead of objects
 * polled from a concurrent queue.</p>
 *
 * <p>Vanilla's LCG is a single shared stream, so the values it would have drawn are still generated in order on the
 * server thread while the chunks are walked; the workers only turn them into positions and look the blocks up.</p>
 */
public class ParallelRandomBlockTicker {

    public static final ParallelRandomBlockTicker INSTANCE = new ParallelRandomBlockTicker();

    private static final int INITIAL_SECTIONS = 2048;

    // Batch, written by the server thread while the chunks are walked, read by the workers afterwards.
    private int[] lcgValues = new int[INITIAL_SECTIONS * 4];
    private Object[] sections = new Object[INITIAL_SECTIONS];
    private int[] chunkXs = new int[INITIAL_SECTIONS];
    private int[] chunkZs = new int[INITIAL_SECTIONS];
    private int sectionsCount = 0;
    private int tickSpeed = 1;

    // Results, one set of buffers per worker slice; a slice is only ever touched by the worker that owns it.
    private final List<LongList> sliceChunkXZ = new ObjectArrayList<>();
    private final List<IntList> sliceRelPos = new ObjectArrayList<>();
    private final List<List<IBlockState>> sliceStates = new ObjectArrayList<>();

    private World world;
    private Random rand;
    private Profiler profiler;

    private ParallelRandomBlockTicker() {
    }

    /**
     * Appends the section vanilla would have random ticked next together with the LCG values it would have drawn for
     * it, and returns the advanced LCG state. Server thread only, called while the chunks of the tick are walked.
     */
    public int enqueueSection(final ExtendedBlockStorage section, final int chunkX, final int chunkZ,
                              final int tickSpeed, int updateLCG) {
        this.tickSpeed = tickSpeed;
        final int index = this.sectionsCount;
        this.ensureSectionCapacity(index + 1);
        this.sections[index] = section;
        this.chunkXs[index] = chunkX;
        this.chunkZs[index] = chunkZ;

        final int base = index * tickSpeed;
        if (base + tickSpeed > this.lcgValues.length) {
            int capacity = this.lcgValues.length;
            while (capacity < base + tickSpeed) {
                capacity <<= 1;
            }
            this.lcgValues = Arrays.copyOf(this.lcgValues, capacity);
        }
        for (int i = 0; i < tickSpeed; i++) {
            updateLCG = updateLCG * 3 + 0x3c6ef35f;
            this.lcgValues[base + i] = updateLCG;
        }

        this.sectionsCount = index + 1;
        return updateLCG;
    }

    public void execute(final World world, final Random rand, final Profiler profiler) {
        final int sections = this.sectionsCount;
        // Take the batch over before doing anything else: if something below throws, the next tick must not replay it.
        this.sectionsCount = 0;
        if (sections == 0) {
            return;
        }
        this.world = world;
        this.rand = rand;
        this.profiler = profiler;

        final int slices = Math.min(StellarEnvironment.getConcurrency(), sections);
        this.prepareSlices(slices);

        if (slices == 1) {
            this.collectSlice(0, 0, sections);
        } else {
            final int perSlice = (sections + slices - 1) / slices;
            IntStream.range(0, slices).parallel().forEach(slice -> {
                final int from = slice * perSlice;
                if (from < sections) {
                    this.collectSlice(slice, from, Math.min(sections, from + perSlice));
                }
            });
        }

        this.profiler.startSection("randomTick");
        for (int slice = 0; slice < slices; slice++) {
            this.randomTickSlice(slice);
        }
        this.profiler.endSection();
    }

    private void ensureSectionCapacity(final int wanted) {
        if (wanted <= this.sections.length) {
            return;
        }
        int capacity = this.sections.length;
        while (capacity < wanted) {
            capacity <<= 1;
        }
        this.sections = Arrays.copyOf(this.sections, capacity);
        this.chunkXs = Arrays.copyOf(this.chunkXs, capacity);
        this.chunkZs = Arrays.copyOf(this.chunkZs, capacity);
    }

    private void prepareSlices(final int slices) {
        while (this.sliceChunkXZ.size() < slices) {
            this.sliceChunkXZ.add(new LongArrayList());
            this.sliceRelPos.add(new IntArrayList());
            this.sliceStates.add(new ObjectArrayList<>());
        }
        for (int slice = 0; slice < slices; slice++) {
            this.sliceChunkXZ.get(slice).clear();
            this.sliceRelPos.get(slice).clear();
            this.sliceStates.get(slice).clear();
        }
    }

    /** Worker side: turns the LCG values of an index range into the blocks that tick randomly. */
    private void collectSlice(final int slice, final int from, final int to) {
        final LongList chunkXZ = this.sliceChunkXZ.get(slice);
        final IntList relPos = this.sliceRelPos.get(slice);
        final List<IBlockState> states = this.sliceStates.get(slice);
        final int tickSpeed = this.tickSpeed;
        for (int index = from; index < to; index++) {
            final ExtendedBlockStorage section = (ExtendedBlockStorage) this.sections[index];
            final long packedChunk = (long) this.chunkXs[index] << 32 | this.chunkZs[index] & 0xFFFFFFFFL;
            final int sectionY = section.getYLocation() >> 4;
            final int base = index * tickSpeed;
            for (int i = 0; i < tickSpeed; i++) {
                final int lcg = this.lcgValues[base + i] >> 2;
                final int x = lcg & 15;
                final int y = lcg >> 16 & 15;
                final int z = lcg >> 8 & 15;
                final IBlockState state = section.get(x, y, z);
                if (state.getBlock().getTickRandomly()) {
                    chunkXZ.add(packedChunk);
                    relPos.add(x | y << 4 | z << 8 | sectionY << 12);
                    states.add(state);
                }
            }
        }
    }

    /** Server thread: runs the collected random ticks, slice by slice. */
    private void randomTickSlice(final int slice) {
        final LongList chunkXZ = this.sliceChunkXZ.get(slice);
        final IntList relPos = this.sliceRelPos.get(slice);
        final List<IBlockState> states = this.sliceStates.get(slice);
        final World world = this.world;
        final Random rand = this.rand;
        for (int i = 0, size = chunkXZ.size(); i < size; i++) {
            final long packedChunk = chunkXZ.getLong(i);
            final int packedPos = relPos.getInt(i);
            final int x = packedPos & 15;
            final int y = (packedPos >> 4 & 15) | (packedPos >> 12 & 15) << 4;
            final int z = packedPos >> 8 & 15;
            final BlockPos pos = new BlockPos((int) (packedChunk >> 32) * 16 + x, y, (int) packedChunk * 16 + z);
            // The block passed getTickRandomly() when the candidate was collected and nothing can have touched the
            // section since, so the state is used exactly as it was read.
            final IBlockState state = states.get(i);
            state.getBlock().randomTick(world, pos, state, rand);
        }
    }

}
