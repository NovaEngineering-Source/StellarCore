package github.kasuminova.stellarcore.mixin.minecraft.resources;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LanguageLoadingReplayTest {
    private static final String FML = "net/minecraftforge/fml/common/FMLCommonHandler";
    private static final String LOCALE = "net/minecraft/client/resources/Locale";
    private static final String MIXIN_BASE = "github/kasuminova/stellarcore/mixin/minecraft/";
    private static final String LANGUAGE_DESCRIPTOR = "(Ljava/util/Map;Ljava/io/InputStream;)Ljava/io/InputStream;";

    @Test
    void installedForgeAndLocaleAgreeOnLanguageParsingAndMapCallbackOrder() throws Exception {
        final Runner original = new Runner(false);
        final Runner candidate = new Runner(true);
        final String[] texts = {
                "", "# ordinary comment\na=b\na=c\n=empty key\nempty=\ntail=without newline",
                "# PARSE_ESCAPES\r\na=hello\\nworld\nspaces\\ key=value\nutf=\\u4f60\\u597d",
                "#\tPARSE_ESCAPES\u0000\na:colon value\ncontinuation=first\\\n second\n",
                " # PARSE_ESCAPES\na=b\n", "\ufeff#PARSE_ESCAPES\na=\u4f60\u597d\n",
                "##PARSE_ESCAPES\ra=b\r", "#PARSE_ESCAPES\ninvalid=\\u12xz\n",
                "percent=%d/%1$05.2f/%s/%1$..f\nwith=equals=kept\n spaced = value \n"
        };
        for (final String text : texts) {
            for (final int chunk : new int[]{1, 2, 13, 8192}) {
                compare(original, candidate, text.getBytes(StandardCharsets.UTF_8), chunk, -1);
            }
        }
        compare(original, candidate, new byte[]{'#', (byte) 0xc0, '\r', '#', 'P', 'A', 'R', 'S', 'E', '_',
                'E', 'S', 'C', 'A', 'P', 'E', 'S', '\n', 'a', '=', (byte) 0xff}, 1, -1);
    }

    @Test
    void largeMixedLineEndingsAndReadFailuresDoNotChangePartialPublicationOrStreamOwnership() throws Exception {
        final Runner original = new Runner(false);
        final Runner candidate = new Runner(true);
        final Random random = new Random(1906);
        final String[] samples = {"", "#comment", "missing", "=empty key", "a=", "a=b=c", " space = kept ",
                "unicode=\u4f60\u597d\ud83d\ude42", "format=%1$05.2f/%d/%s", "bad=%1$..f", "\ufeffbom=value"};
        final String[] endings = {"\n", "\r", "\r\n"};
        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            text.append(samples[random.nextInt(samples.length)]).append(endings[random.nextInt(endings.length)]);
        }
        text.append("tail=without newline");
        final byte[] bytes = text.toString().getBytes(StandardCharsets.UTF_8);
        compare(original, candidate, bytes, 13, -1);
        for (final int failAfter : new int[]{0, 1, 9, 8191, 8192, 10001}) {
            compare(original, candidate, bytes, 13, failAfter);
        }
    }

    private static void compare(final Runner original, final Runner candidate, final byte[] bytes,
                                final int chunk, final int failAfter) throws Exception {
        final Outcome expected = original.run(bytes, chunk, failAfter);
        final Outcome actual = candidate.run(bytes, chunk, failAfter);
        assertEquals(expected.properties, actual.properties);
        assertEquals(expected.writes, actual.writes, "Map.put callback order");
        assertEquals(expected.failure, actual.failure);
        assertFalse(expected.closed);
        assertFalse(actual.closed);
    }

    private static ClassNode read(final String resource) throws Exception {
        try (InputStream input = LanguageLoadingReplayTest.class.getClassLoader().getResourceAsStream(resource + ".class")) {
            assertNotNull(input, resource);
            final ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            return node;
        }
    }

    private static MethodNode method(final ClassNode owner, final String name, final String descriptor) {
        for (final MethodNode method : owner.methods) {
            if ((name == null || method.name.equals(name)) && method.desc.equals(descriptor)) return method;
        }
        throw new AssertionError("Method not found: " + owner.name + "." + name + descriptor);
    }

    private static final class Runner {
        private final Class<?> isolated;
        private final Method apply;

        private Runner(final boolean optimized) throws Exception {
            final ClassNode locale = read(optimized ? MIXIN_BASE + "resources/MixinLocaleParsing" : LOCALE);
            final ClassNode fml = read(optimized ? MIXIN_BASE + "forge/MixinFMLLanguageLoading" : FML);
            final ClassNode target = new ClassNode();
            target.version = Opcodes.V1_8;
            target.access = Opcodes.ACC_PUBLIC;
            target.name = "github/kasuminova/stellarcore/replay/" + (optimized ? "LanguageCandidate" : "LanguageOriginal");
            target.superName = "java/lang/Object";
            target.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "properties", "Ljava/util/Map;", null, null));
            if (!optimized) {
                for (final FieldNode field : locale.fields) {
                    if ((field.access & Opcodes.ACC_STATIC) != 0) target.fields.add(field);
                }
                for (final MethodNode method : locale.methods) {
                    if (method.name.equals("<clinit>")) {
                        rewriteOwners(method, locale.name, target.name);
                        target.methods.add(method);
                    }
                }
            }
            final MethodNode loadLanguage = method(fml, "loadLanguage", LANGUAGE_DESCRIPTOR);
            // Both implementations use no instance state; isolate the real method without FML's global initializer.
            loadLanguage.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
            loadLanguage.visibleAnnotations = null;
            loadLanguage.invisibleAnnotations = null;
            for (AbstractInsnNode instruction = loadLanguage.instructions.getFirst(); instruction != null;
                 instruction = instruction.getNext()) {
                if (instruction instanceof VarInsnNode) {
                    final VarInsnNode local = (VarInsnNode) instruction;
                    if (local.var == 0) throw new AssertionError("loadLanguage started using its receiver");
                    local.var--;
                } else if (instruction instanceof IincInsnNode) {
                    ((IincInsnNode) instruction).var--;
                }
            }
            loadLanguage.maxLocals--;
            target.methods.add(loadLanguage);
            final MethodNode loadLocale = method(locale, null, "(Ljava/io/InputStream;)V");
            loadLocale.name = "apply";
            loadLocale.access = Opcodes.ACC_PUBLIC;
            loadLocale.visibleAnnotations = null;
            loadLocale.invisibleAnnotations = null;
            rewriteOwners(loadLocale, locale.name, target.name);
            for (AbstractInsnNode instruction = loadLocale.instructions.getFirst(); instruction != null;) {
                final AbstractInsnNode next = instruction.getNext();
                if (instruction instanceof MethodInsnNode) {
                    final MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals(FML) && call.name.equals("instance")) {
                        loadLocale.instructions.remove(call);
                    } else if (call.owner.equals(FML) && call.name.equals("loadLanguage")) {
                        call.setOpcode(Opcodes.INVOKESTATIC);
                        call.owner = target.name;
                        call.itf = false;
                    }
                }
                instruction = next;
            }
            target.methods.add(loadLocale);
            final MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
            constructor.instructions.add(new InsnNode(Opcodes.RETURN));
            target.methods.add(constructor);
            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            target.accept(writer);
            final byte[] bytes = writer.toByteArray();
            isolated = new ClassLoader(LanguageLoadingReplayTest.class.getClassLoader()) {
                Class<?> define() { return defineClass(target.name.replace('/', '.'), bytes, 0, bytes.length); }
            }.define();
            apply = isolated.getMethod("apply", InputStream.class);
        }

        private Outcome run(final byte[] bytes, final int chunk, final int failAfter) throws Exception {
            final Object instance = isolated.getDeclaredConstructor().newInstance();
            final TracingMap properties = new TracingMap();
            properties.put("existing", "value");
            properties.writes.clear();
            isolated.getField("properties").set(instance, properties);
            final Source stream = new Source(bytes, chunk, failAfter);
            Class<?> failure = null;
            try {
                apply.invoke(instance, stream);
            } catch (InvocationTargetException ex) {
                failure = ex.getCause().getClass();
            }
            return new Outcome(properties, properties.writes, failure, stream.closed);
        }
    }

    private static void rewriteOwners(final MethodNode method, final String original, final String target) {
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof FieldInsnNode) {
                final FieldInsnNode field = (FieldInsnNode) instruction;
                if (field.owner.equals(original)) {
                    field.owner = target;
                    if (field.getOpcode() == Opcodes.GETFIELD && field.desc.equals("Ljava/util/Map;")) field.name = "properties";
                }
            } else if (instruction instanceof MethodInsnNode) {
                final MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.equals(original)) call.owner = target;
            } else if (instruction instanceof TypeInsnNode) {
                final TypeInsnNode type = (TypeInsnNode) instruction;
                if (type.desc.equals(original)) type.desc = target;
            }
        }
    }

    private static final class TracingMap extends Object2ObjectOpenHashMap<String, String> {
        private final List<String> writes = new ObjectArrayList<>();

        @Override
        public String put(final String key, final String value) {
            writes.add(key + '=' + value);
            return super.put(key, value);
        }
    }

    private static final class Source extends InputStream {
        private final byte[] bytes;
        private final int chunk;
        private final int failAfter;
        private int offset;
        private boolean closed;

        private Source(final byte[] bytes, final int chunk, final int failAfter) {
            this.bytes = bytes;
            this.chunk = chunk;
            this.failAfter = failAfter;
        }

        @Override
        public int read() throws IOException {
            if (failAfter >= 0 && offset >= failAfter) throw new IOException("read failure");
            return offset < bytes.length ? bytes[offset++] & 255 : -1;
        }

        @Override
        public int read(final byte[] buffer, final int start, final int length) throws IOException {
            if (length == 0) return 0;
            if (failAfter >= 0 && offset >= failAfter) throw new IOException("read failure");
            if (offset == bytes.length) return -1;
            int count = Math.min(chunk, Math.min(length, bytes.length - offset));
            if (failAfter >= 0) count = Math.min(count, failAfter - offset);
            System.arraycopy(bytes, offset, buffer, start, count);
            offset += count;
            return count;
        }

        @Override
        public void close() { closed = true; }
    }

    private static final class Outcome {
        private final Map<String, String> properties;
        private final List<String> writes;
        private final Class<?> failure;
        private final boolean closed;

        private Outcome(final Map<String, String> properties, final List<String> writes,
                        final Class<?> failure, final boolean closed) {
            this.properties = properties;
            this.writes = writes;
            this.failure = failure;
            this.closed = closed;
        }
    }
}
