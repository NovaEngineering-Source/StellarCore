package github.kasuminova.stellarcore.common.world;

import github.kasuminova.stellarcore.common.util.StellarEnvironment;
import net.minecraft.block.state.IBlockState;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/** Reuses one batch of random-tick candidates without retaining worlds or sections between ticks. */
public final class ParallelRandomBlockTicker {
    public static final ParallelRandomBlockTicker INSTANCE = new ParallelRandomBlockTicker();

    private static final int PARALLEL_THRESHOLD = 256;
    private ExtendedBlockStorage[] sections = new ExtendedBlockStorage[256];
    private int[] chunkXs = new int[256];
    private int[] chunkZs = new int[256];
    private int[] starts = new int[256];
    private int[] lengths = new int[256];
    private int[] lcgValues = new int[1024];
    private boolean[] selected = new boolean[1024];
    private int sectionCount;
    private int candidateCount;
    private final Slice[] slices;

    private ParallelRandomBlockTicker() {
        slices = new Slice[Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors()))];
        for (int i = 0; i < slices.length; i++) {
            slices[i] = new Slice();
        }
    }

    public int enqueueSection(final ExtendedBlockStorage section, final int chunkX, final int chunkZ,
                              final int tickSpeed, int updateLCG) {
        if (tickSpeed <= 0) {
            return updateLCG;
        }
        if (sectionCount == sections.length) {
            final int capacity = Math.multiplyExact(sectionCount, 2);
            sections = Arrays.copyOf(sections, capacity);
            chunkXs = Arrays.copyOf(chunkXs, capacity);
            chunkZs = Arrays.copyOf(chunkZs, capacity);
            starts = Arrays.copyOf(starts, capacity);
            lengths = Arrays.copyOf(lengths, capacity);
        }
        final int end = Math.addExact(candidateCount, tickSpeed);
        if (end > lcgValues.length) {
            final int capacity = Math.max(end, Math.multiplyExact(lcgValues.length, 2));
            lcgValues = Arrays.copyOf(lcgValues, capacity);
            selected = Arrays.copyOf(selected, capacity);
        }
        sections[sectionCount] = section;
        chunkXs[sectionCount] = chunkX;
        chunkZs[sectionCount] = chunkZ;
        starts[sectionCount] = candidateCount;
        lengths[sectionCount++] = tickSpeed;
        for (int i = candidateCount; i < end; i++) {
            updateLCG = updateLCG * 3 + 0x3c6ef35f;
            lcgValues[i] = updateLCG;
        }
        candidateCount = end;
        return updateLCG;
    }

    public void execute(final World world, final Random rand, final Profiler profiler) {
        if (sectionCount == 0) {
            return;
        }
        try {
            final int workers = Math.max(1, Math.min(Math.min(StellarEnvironment.getConcurrency(), slices.length), sectionCount));
            if (workers == 1 || candidateCount < PARALLEL_THRESHOLD) {
                select(0, sectionCount);
            } else {
                selectParallel(workers);
            }
            profiler.startSection("randomTick");
            try {
                runSelected(world, rand);
            } finally {
                profiler.endSection();
            }
        } finally {
            Arrays.fill(sections, 0, sectionCount, null);
            sectionCount = 0;
            candidateCount = 0;
        }
    }

    private void selectParallel(final int workers) {
        final int stride = (sectionCount + workers - 1) / workers;
        int submitted = 0;
        Throwable failure = null;
        try {
            for (int i = 1; i < workers; i++) {
                final Slice slice = slices[i];
                slice.from = Math.min(sectionCount, i * stride);
                slice.to = Math.min(sectionCount, (i + 1) * stride);
                ForkJoinPool.commonPool().execute(slice);
                submitted++;
            }
            select(0, Math.min(stride, sectionCount));
        } catch (final Throwable thrown) {
            failure = thrown;
        } finally {
            for (int i = 1; i <= submitted; i++) {
                try {
                    slices[i].join();
                } catch (final Throwable thrown) {
                    if (failure == null) {
                        failure = thrown;
                    } else if (failure != thrown) {
                        failure.addSuppressed(thrown);
                    }
                } finally {
                    slices[i].reinitialize();
                }
            }
        }
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (failure != null) {
            throw new IllegalStateException("Random-tick selection failed", failure);
        }
    }

    private void select(final int from, final int to) {
        for (int sectionIndex = from; sectionIndex < to; sectionIndex++) {
            final ExtendedBlockStorage section = sections[sectionIndex];
            final int end = starts[sectionIndex] + lengths[sectionIndex];
            for (int i = starts[sectionIndex]; i < end; i++) {
                final int value = lcgValues[i] >> 2;
                selected[i] = section.get(value & 15, value >> 16 & 15, value >> 8 & 15)
                    .getBlock().getTickRandomly();
            }
        }
    }

    private void runSelected(final World world, final Random rand) {
        for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
            final ExtendedBlockStorage section = sections[sectionIndex];
            final int end = starts[sectionIndex] + lengths[sectionIndex];
            for (int i = starts[sectionIndex]; i < end; i++) {
                if (!selected[i]) {
                    continue;
                }
                final int value = lcgValues[i] >> 2;
                final int x = value & 15;
                final int y = value >> 16 & 15;
                final int z = value >> 8 & 15;
                final IBlockState state = section.get(x, y, z);
                if (state.getBlock().getTickRandomly()) {
                    state.getBlock().randomTick(world,
                        new BlockPos((chunkXs[sectionIndex] << 4) + x, section.getYLocation() + y,
                            (chunkZs[sectionIndex] << 4) + z), state, rand);
                }
            }
        }
    }

    private final class Slice extends RecursiveAction {
        private int from;
        private int to;

        @Override
        protected void compute() {
            select(from, to);
        }
    }
}
