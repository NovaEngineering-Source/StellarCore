package github.kasuminova.stellarcore.client.integration.libnine;

import net.minecraft.util.ResourceLocation;

import java.util.function.Function;

/** A single model-loader selection sees one LibNine type for its model. */
public final class L9ModelSelection implements AutoCloseable {
    private static final ThreadLocal<L9ModelSelection> CURRENT = new ThreadLocal<>();
    private static volatile Object generation;

    private final Object ownerGeneration;
    private final L9ModelSelection previous;
    private ResourceLocation location;
    private Object value;

    private L9ModelSelection(final Object ownerGeneration, final L9ModelSelection previous) {
        this.ownerGeneration = ownerGeneration;
        this.previous = previous;
    }

    public static synchronized void enable() {
        generation = new Object();
    }

    public static synchronized void reload() {
        if (generation != null) {
            generation = new Object();
        }
    }

    public static synchronized void disable() {
        generation = null;
    }

    public static L9ModelSelection open() {
        final Object active = generation;
        if (active == null) {
            return null;
        }
        final L9ModelSelection selection = new L9ModelSelection(active, CURRENT.get());
        CURRENT.set(selection);
        return selection;
    }

    /** Null is a transient read/parse failure. A confirmed missing key can have a non-null sentinel. */
    public static Object read(final ResourceLocation resource, final Function<ResourceLocation, Object> reader) {
        final L9ModelSelection selection = CURRENT.get();
        if (selection == null || selection.ownerGeneration != generation) {
            return reader.apply(resource);
        }
        if (selection.location != null) {
            return selection.location.equals(resource) ? selection.value : reader.apply(resource);
        }
        final Object result = reader.apply(resource);
        if (result != null) {
            selection.location = resource;
            selection.value = result;
        }
        return result;
    }

    @Override
    public void close() {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
