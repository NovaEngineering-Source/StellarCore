package github.kasuminova.stellarcore.client.memory;

/**
 * Initial capacity only: vanilla BufferBuilder retains its normal growth path.
 */
public final class RenderBufferCapacity {
    public static final int INITIAL_INTS = 16 * 1024;

    private RenderBufferCapacity() {
    }

    public static int initialInts(final int requested) {
        return Math.min(requested, INITIAL_INTS);
    }
}
