package github.kasuminova.stellarcore.client.integration.enderutilities;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NullifierQuadFlagsTest {
    private static final String BRIDGE = "github.kasuminova.stellarcore.client.integration.enderutilities.NullifierQuadFlags";
    private static final String VIEW = "dhj.embeddedt.embeddium.impl.model.quad.BakedQuadView";
    private static final String FIXTURE = "fixture.NullifierFlaggedQuad";
    private static final String QUAD = "net/minecraft/client/renderer/block/model/BakedQuad";
    private static final String CONSTRUCTOR = "([IILnet/minecraft/util/EnumFacing;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;ZLnet/minecraft/client/renderer/vertex/VertexFormat;)V";

    @Test
    void vanillaPathDoesNotRequireActiniumClasses() throws Exception {
        final Class<?> bridge = new IsolatedFlags(false).loadClass(BRIDGE);
        final BakedQuad plain = plain();
        assertEquals(0, read(bridge, plain));
        copy(bridge, plain, plain, -1);
    }

    @Test
    void optionalBridgeRetainsProducerFlagsAndDropsMaskedGeometryBits() throws Exception {
        final IsolatedFlags loader = new IsolatedFlags(true);
        final Class<?> bridge = loader.loadClass(BRIDGE);
        final Class<?> quadType = loader.loadClass(FIXTURE);
        final BakedQuad source = flagged(quadType), target = flagged(quadType);
        quadType.getField("flags").setInt(source, Integer.MIN_VALUE | 1 | 2 | 4 | 32 | 16);
        assertEquals(Integer.MIN_VALUE | 1 | 2 | 4 | 32 | 16, read(bridge, source));
        copy(bridge, source, target, ~(Integer.MIN_VALUE | 1 | 2 | 4 | 32));
        assertEquals(16, quadType.getField("flags").getInt(target));
        assertEquals(0, read(bridge, plain()));
        copy(bridge, source, plain(), -1);
        copy(bridge, plain(), target, -1);
        assertEquals(16, quadType.getField("flags").getInt(target));
    }

    private static BakedQuad plain() {
        return new BakedQuad(new int[DefaultVertexFormats.ITEM.getIntegerSize() * 4], -1,
            EnumFacing.UP, null, true, DefaultVertexFormats.ITEM);
    }

    private static BakedQuad flagged(final Class<?> type) throws Exception {
        return (BakedQuad) type.getConstructor(int[].class, int.class, EnumFacing.class,
            TextureAtlasSprite.class, boolean.class, VertexFormat.class).newInstance(
            new int[DefaultVertexFormats.ITEM.getIntegerSize() * 4], -1,
            EnumFacing.UP, null, true, DefaultVertexFormats.ITEM);
    }

    private static int read(final Class<?> bridge, final BakedQuad quad) throws Exception {
        final Method method = bridge.getDeclaredMethod("get", BakedQuad.class);
        method.setAccessible(true);
        return (int) method.invoke(null, quad);
    }

    private static void copy(final Class<?> bridge, final BakedQuad source, final BakedQuad target,
                             final int mask) throws Exception {
        final Method method = bridge.getDeclaredMethod("copyMasked", BakedQuad.class, BakedQuad.class, int.class);
        method.setAccessible(true);
        method.invoke(null, source, target, mask);
    }

    private static final class IsolatedFlags extends ClassLoader {
        private final boolean present;

        private IsolatedFlags(final boolean present) {
            super(NullifierQuadFlagsTest.class.getClassLoader());
            this.present = present;
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> found = findLoadedClass(name);
                if (found == null) {
                    if (name.equals(VIEW)) {
                        if (!present) throw new ClassNotFoundException(name);
                        final byte[] bytes = view();
                        found = defineClass(name, bytes, 0, bytes.length);
                    } else if (name.equals(FIXTURE)) {
                        final byte[] bytes = flaggedQuad();
                        found = defineClass(name, bytes, 0, bytes.length);
                    } else if (name.equals(BRIDGE) || name.startsWith(BRIDGE + "$")) {
                        try (InputStream input = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                            if (input == null) throw new ClassNotFoundException(name);
                            final ByteArrayOutputStream output = new ByteArrayOutputStream();
                            final byte[] buffer = new byte[4096];
                            int count;
                            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
                            final byte[] bytes = output.toByteArray();
                            found = defineClass(name, bytes, 0, bytes.length);
                        } catch (IOException failure) {
                            throw new ClassNotFoundException(name, failure);
                        }
                    } else found = super.loadClass(name, false);
                }
                if (resolve) resolveClass(found);
                return found;
            }
        }
    }

    private static byte[] view() {
        final ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
            VIEW.replace('.', '/'), null, "java/lang/Object", null);
        writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "getFlags", "()I", null, null).visitEnd();
        writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "addFlags", "(I)V", null, null).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] flaggedQuad() {
        final String name = FIXTURE.replace('.', '/');
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, QUAD, new String[]{VIEW.replace('.', '/')});
        writer.visitField(Opcodes.ACC_PUBLIC, "flags", "I", null, null).visitEnd();
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", CONSTRUCTOR, null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitVarInsn(Opcodes.ILOAD, 2);
        method.visitVarInsn(Opcodes.ALOAD, 3);
        method.visitVarInsn(Opcodes.ALOAD, 4);
        method.visitVarInsn(Opcodes.ILOAD, 5);
        method.visitVarInsn(Opcodes.ALOAD, 6);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, QUAD, "<init>", CONSTRUCTOR, false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        method = writer.visitMethod(Opcodes.ACC_PUBLIC, "getFlags", "()I", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitFieldInsn(Opcodes.GETFIELD, name, "flags", "I");
        method.visitInsn(Opcodes.IRETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        method = writer.visitMethod(Opcodes.ACC_PUBLIC, "addFlags", "(I)V", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitInsn(Opcodes.DUP);
        method.visitFieldInsn(Opcodes.GETFIELD, name, "flags", "I");
        method.visitVarInsn(Opcodes.ILOAD, 1);
        method.visitInsn(Opcodes.IOR);
        method.visitFieldInsn(Opcodes.PUTFIELD, name, "flags", "I");
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
