package github.kasuminova.stellarcore.mixin.geckolib;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GeckoZipResourcePrefixesReplayTest {
    @TempDir
    Path temporary;

    @Test
    void discoveryAndStatefulPredicateOrderMatchInstalledGeckoLib() throws Exception {
        final Path archive = archive();
        for (final String directory : new String[]{"animations", "geo", "missing", ""}) {
            compare(archive, directory, 0);
            compare(archive, directory, 1);
            compare(archive, directory, 2);
        }
    }

    @Test
    void predicateCanExtendTheLiveDomainSetOrThrowWithoutReorderingCallbacks() throws Exception {
        final Path archive = archive();
        compare(archive, "animations", 3);
        compare(archive, "animations", 4);
    }

    private void compare(final Path archive, final String directory, final int mode) throws Exception {
        final Outcome expected = run(archive, directory, mode, false);
        final Outcome actual = run(archive, directory, mode, true);
        assertEquals(expected.calls, actual.calls, "predicate callback order");
        assertEquals(expected.locations, actual.locations, "resource multiplicity and order");
        assertEquals(expected.failure, actual.failure);
        assertEquals(expected.domainsRead, actual.domainsRead);
        assertEquals(1, actual.domainsRead);
    }

    private Outcome run(final Path archive, final String directory, final int mode, final boolean optimized)
            throws Exception {
        final LiveDomains domains = new LiveDomains();
        domains.add("first");
        domains.add("second");
        final Pack pack = new Pack(archive, domains);
        final List<String> calls = new ObjectArrayList<>();
        final List<ResourceLocation> locations = new ObjectArrayList<>();
        final Predicate<String> predicate = name -> {
            calls.add(name);
            if (mode == 3 && calls.size() == 1) domains.add("late");
            if (mode == 4 && calls.size() == 2) throw new IllegalStateException("predicate failure");
            return mode != 1 && (mode != 2 || calls.size() % 3 != 0);
        };
        final Object instance;
        final Method method;
        if (optimized) {
            instance = new MixinGeckoZipResourcePrefixes() { };
            method = MixinGeckoZipResourcePrefixes.class.getDeclaredMethod("enumerateZipFile",
                    FileResourcePack.class, String.class, ZipFile.class, Predicate.class, List.class);
            method.setAccessible(true);
        } else {
            final Class<?> baseline = baseline();
            instance = baseline.getDeclaredConstructor().newInstance();
            method = baseline.getMethod("enumerate", FileResourcePack.class, String.class,
                    ZipFile.class, Predicate.class, List.class);
        }
        Class<?> failure = null;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            try {
                method.invoke(instance, pack, directory, zip, predicate, locations);
            } catch (InvocationTargetException ex) {
                failure = ex.getCause().getClass();
            }
        }
        return new Outcome(calls, locations, failure, pack.reads);
    }

    private Path archive() throws Exception {
        final Path archive = temporary.resolve("resources.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (final String path : new String[]{
                    "assets/first/animations/a.json", "assets/second/geo/model.json", "unrelated.txt",
                    "assets/late/animations/visible.json", "assets/second/animations/b.json",
                    "assets/first/animations/", "assets/first/animations/nested/c.json",
                    "assets/first/geo/geometry.json", "assets/second/animations/not_json.txt",
                    "assets/first/animations-other/x.json", "assets/second//empty-directory.json"}) {
                output.putNextEntry(new ZipEntry(path));
                output.closeEntry();
            }
        }
        return archive;
    }

    private static Class<?> baseline() throws Exception {
        final ClassNode original = new ClassNode();
        try (InputStream input = GeckoZipResourcePrefixesReplayTest.class.getClassLoader()
                .getResourceAsStream("software/bernie/geckolib3/resource/GeckoLibCache.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(original, ClassReader.SKIP_FRAMES);
        }
        MethodNode enumerate = null;
        for (final MethodNode method : original.methods) {
            if (method.name.equals("enumerateZipFile")) enumerate = method;
        }
        assertNotNull(enumerate);
        final ClassNode isolated = new ClassNode();
        isolated.version = Opcodes.V1_8;
        isolated.access = Opcodes.ACC_PUBLIC;
        isolated.name = "github/kasuminova/stellarcore/replay/GeckoEnumeration";
        isolated.superName = "java/lang/Object";
        // Isolate only the real dependency method, without GeckoLib's singleton initialization.
        enumerate.access = Opcodes.ACC_PUBLIC;
        enumerate.name = "enumerate";
        isolated.methods.add(enumerate);
        final MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        isolated.methods.add(constructor);
        final ClassWriter output = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        isolated.accept(output);
        final byte[] bytes = output.toByteArray();
        return new ClassLoader(GeckoZipResourcePrefixesReplayTest.class.getClassLoader()) {
            Class<?> define() { return defineClass(isolated.name.replace('/', '.'), bytes, 0, bytes.length); }
        }.define();
    }

    private static final class Pack extends FileResourcePack {
        private final Set<String> domains;
        private int reads;

        private Pack(final Path archive, final Set<String> domains) {
            super(archive.toFile());
            this.domains = domains;
        }

        @Override
        public Set<String> getResourceDomains() {
            reads++;
            return domains;
        }
    }

    private static final class LiveDomains extends AbstractSet<String> {
        private final List<String> values = new ObjectArrayList<>();

        @Override
        public boolean add(final String value) {
            if (values.contains(value)) return false;
            values.add(value);
            return true;
        }

        @Override
        public int size() { return values.size(); }

        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                private int next;
                @Override public boolean hasNext() { return next < values.size(); }
                @Override public String next() { return values.get(next++); }
            };
        }
    }

    private static final class Outcome {
        private final List<String> calls;
        private final List<ResourceLocation> locations;
        private final Class<?> failure;
        private final int domainsRead;

        private Outcome(final List<String> calls, final List<ResourceLocation> locations,
                        final Class<?> failure, final int domainsRead) {
            this.calls = calls;
            this.locations = locations;
            this.failure = failure;
            this.domainsRead = domainsRead;
        }
    }
}
