package github.kasuminova.stellarcore.client.integration.enderutilities;

import net.minecraft.client.renderer.block.model.BakedQuad;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

final class NullifierQuadFlags {
    private static final Access ACCESS = Access.load();

    private NullifierQuadFlags() {
    }

    static int get(final BakedQuad quad) {
        final Access access = ACCESS;
        if (access == null || !access.view.isInstance(quad)) return 0;
        try {
            return (int) access.get.invokeExact(quad);
        } catch (Throwable failure) {
            throw accessFailure(failure);
        }
    }

    static void copyMasked(final BakedQuad source, final BakedQuad target, final int mask) {
        final Access access = ACCESS;
        if (access == null || !access.view.isInstance(source) || !access.view.isInstance(target)) return;
        try {
            final int flags = (int) access.get.invokeExact(source) & mask;
            if (flags != 0) access.add.invokeExact(target, flags);
        } catch (Throwable failure) {
            throw accessFailure(failure);
        }
    }

    private static IllegalStateException accessFailure(final Throwable failure) {
        if (failure instanceof Error) throw (Error) failure;
        if (failure instanceof RuntimeException) throw (RuntimeException) failure;
        return new IllegalStateException("Unable to access Actinium quad flags", failure);
    }

    private static final class Access {
        private final Class<?> view;
        private final MethodHandle get;
        private final MethodHandle add;

        private Access(final Class<?> view, final MethodHandle get, final MethodHandle add) {
            this.view = view;
            this.get = get;
            this.add = add;
        }

        private static Access load() {
            final Class<?> view;
            try {
                view = Class.forName("dhj.embeddedt.embeddium.impl.model.quad.BakedQuadView", false,
                    NullifierQuadFlags.class.getClassLoader());
            } catch (ClassNotFoundException absent) {
                return null;
            }
            try {
                final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                final MethodHandle get = lookup.findVirtual(view, "getFlags", MethodType.methodType(int.class))
                    .asType(MethodType.methodType(int.class, BakedQuad.class));
                final MethodHandle add = lookup.findVirtual(view, "addFlags", MethodType.methodType(void.class, int.class))
                    .asType(MethodType.methodType(void.class, BakedQuad.class, int.class));
                return new Access(view, get, add);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Unsupported Actinium quad flag API", failure);
            }
        }
    }
}
