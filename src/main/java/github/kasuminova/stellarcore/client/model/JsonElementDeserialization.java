package github.kasuminova.stellarcore.client.model;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.internal.bind.TreeTypeAdapter;
import com.google.gson.reflect.TypeToken;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;

/** Reuses tree readers only for adapters which never expose a reader to user code. */
public final class JsonElementDeserialization {

    private static final int MAX_DEPTH = 16;
    private static final int MAX_RETAINED_STACK = 256;
    private static final Access ACCESS = Access.create();
    private static final ThreadLocal<Readers> READERS = ThreadLocal.withInitial(Readers::new);

    private JsonElementDeserialization() {
    }

    public static <T> T deserialize(final JsonDeserializationContext context, final JsonElement json, final Type type) {
        final Access access = ACCESS;
        if (access == null || !access.enabled || context == null || json == null || type == null
            || context.getClass() != access.contextClass) {
            return context.deserialize(json, type);
        }

        final Gson gson;
        final Map<?, ?> adapters;
        try {
            gson = (Gson) access.contextGson.invokeExact(context);
            adapters = (Map<?, ?>) access.adapters.invokeExact(gson);
        } catch (Throwable unsupported) {
            rethrowUnchecked(unsupported);
            access.enabled = false;
            return context.deserialize(json, type);
        }

        final Object adapter = adapters.get(TypeToken.get(type));
        if (adapter == null || adapter.getClass() != TreeTypeAdapter.class) {
            return context.deserialize(json, type);
        }
        final boolean hasDeserializer;
        try {
            hasDeserializer = (JsonDeserializer<?>) access.deserializer.invokeExact((TreeTypeAdapter<?>) adapter) != null;
        } catch (Throwable unsupported) {
            rethrowUnchecked(unsupported);
            access.enabled = false;
            return context.deserialize(json, type);
        }
        if (!hasDeserializer) {
            return context.deserialize(json, type);
        }

        final Readers readers = READERS.get();
        final int depth = readers.depth;
        if (depth == MAX_DEPTH) {
            return context.deserialize(json, type);
        }
        final JsonTreeReader reader;
        try {
            reader = readers.acquire(depth, json, access);
        } catch (Throwable unsupported) {
            rethrowUnchecked(unsupported);
            access.enabled = false;
            readers.readers[depth] = null;
            return context.deserialize(json, type);
        }
        readers.depth = depth + 1;
        try {
            // Keep Gson's public read path: strictness, null handling, custom callbacks,
            // exception wrapping and result-type checks remain owned by Gson.
            return gson.fromJson(reader, type);
        } finally {
            readers.depth = depth;
            readers.release(depth, reader, access);
        }
    }

    private static void rethrowUnchecked(final Throwable failure) {
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }
        if (failure instanceof Error) {
            throw (Error) failure;
        }
    }

    private static final class Readers {
        private final JsonTreeReader[] readers = new JsonTreeReader[MAX_DEPTH];
        private int depth;

        private JsonTreeReader acquire(final int index, final JsonElement json, final Access access) throws Throwable {
            JsonTreeReader reader = readers[index];
            if (reader == null) {
                reader = new JsonTreeReader(json);
                readers[index] = reader;
            } else {
                final Object[] stack = (Object[]) access.stack.invokeExact(reader);
                stack[0] = json;
                access.setStackSize.invokeExact(reader, 1);
            }
            return reader;
        }

        private void release(final int index, final JsonTreeReader reader, final Access access) {
            try {
                final Object[] stack = (Object[]) access.stack.invokeExact(reader);
                if (stack.length > MAX_RETAINED_STACK) {
                    readers[index] = null;
                    return;
                }
                Arrays.fill(stack, null);
                Arrays.fill((String[]) access.pathNames.invokeExact(reader), null);
                Arrays.fill((int[]) access.pathIndices.invokeExact(reader), 0);
                access.setStackSize.invokeExact(reader, 0);
            } catch (Throwable unsupported) {
                readers[index] = null;
                access.enabled = false;
                rethrowUnchecked(unsupported);
            }
        }
    }

    private static final class Access {
        private final Class<?> contextClass;
        private final MethodHandle contextGson;
        private final MethodHandle adapters;
        private final MethodHandle deserializer;
        private final MethodHandle stack;
        private final MethodHandle setStackSize;
        private final MethodHandle pathNames;
        private final MethodHandle pathIndices;
        private volatile boolean enabled = true;

        private Access(final Class<?> contextClass) throws ReflectiveOperationException {
            this.contextClass = contextClass;
            contextGson = MethodHandles.filterReturnValue(
                getter(contextClass, "this$0", TreeTypeAdapter.class),
                getter(TreeTypeAdapter.class, "gson", Gson.class)
            ).asType(MethodType.methodType(Gson.class, JsonDeserializationContext.class));
            adapters = getter(Gson.class, "typeTokenCache", Map.class);
            deserializer = getter(TreeTypeAdapter.class, "deserializer", JsonDeserializer.class);
            stack = getter(JsonTreeReader.class, "stack", Object[].class);
            setStackSize = MethodHandles.lookup().unreflectSetter(field(JsonTreeReader.class, "stackSize", int.class));
            pathNames = getter(JsonTreeReader.class, "pathNames", String[].class);
            pathIndices = getter(JsonTreeReader.class, "pathIndices", int[].class);
        }

        private static MethodHandle getter(final Class<?> owner, final String name, final Class<?> type) throws ReflectiveOperationException {
            return MethodHandles.lookup().unreflectGetter(field(owner, name, type))
                .asType(MethodType.methodType(type, owner));
        }

        private static Field field(final Class<?> owner, final String name, final Class<?> type) throws ReflectiveOperationException {
            final Field field = owner.getDeclaredField(name);
            if (!type.isAssignableFrom(field.getType())) {
                throw new NoSuchFieldException(name);
            }
            field.setAccessible(true);
            return field;
        }

        private static Access create() {
            try {
                for (final Class<?> nested : TreeTypeAdapter.class.getDeclaredClasses()) {
                    if (JsonDeserializationContext.class.isAssignableFrom(nested)) {
                        return new Access(nested);
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
                // Unsupported Gson versions or inaccessible internals retain the original API path.
            }
            return null;
        }
    }
}
