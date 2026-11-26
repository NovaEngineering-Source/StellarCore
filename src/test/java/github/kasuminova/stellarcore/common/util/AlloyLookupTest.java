package github.kasuminova.stellarcore.common.util;

import net.minecraft.item.Item;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AlloyLookupTest {
    /** Supplies only registry identities and the empty-slot sentinel, without MC Bootstrap. */
    public static final class TestItems {
        public static final Item AIR = new Item();
        private static final Item[] VALUES = {AIR, new Item(), new Item(), new Item(), new Item()};

        public static int id(final Item item) {
            for (int i = 0; i < VALUES.length; i++) if (VALUES[i] == item) return i;
            return -1;
        }
    }

    @Test
    void multisetIndexMatchesOriginalPermutationTreeAfterEveryInsertion() throws Exception {
        final ClassLoader loader = lookupLoader();
        final LookupPair pair = new LookupPair(loader);
        final Random random = new Random(1896);
        for (int iteration = 0; iteration < 160; iteration++) {
            // Equality collisions and repeated alternatives must preserve candidate priority.
            final Integer recipe = Integer.valueOf(1000 + iteration % 41);
            pair.add(recipe, inputs(random, 1 + iteration % 3));
            pair.assertEquivalent("after insertion " + iteration);
        }
    }

    @Test
    void rebuildingInTheNativeIteratorOrderPreservesCandidatesAndRecipePriority() throws Exception {
        final ClassLoader loader = lookupLoader();
        final LookupPair initial = new LookupPair(loader);
        final Random random = new Random(8196);
        final List<Item[]> definitions = new ArrayList<>();
        for (int recipe = 0; recipe < 96; recipe++) {
            final Item[] inputs = inputs(random, 1 + recipe % 3);
            definitions.add(inputs);
            initial.add(recipe, inputs);
        }
        initial.assertEquivalent("before rebuild");
        // AlloyRecipeManager.rebuild obtains its registration order from this iterator.
        final LookupPair rebuilt = new LookupPair(loader);
        for (final Object recipe : contents((Iterable<?>) initial.baseline)) {
            rebuilt.add(recipe, definitions.get((Integer) recipe));
        }
        rebuilt.assertEquivalent("after iterator replay");
    }

    private static Item[] inputs(final Random random, final int count) {
        final Item[] result = new Item[count];
        result[0] = TestItems.VALUES[1 + random.nextInt(TestItems.VALUES.length - 1)];
        for (int i = 1; i < count; i++) result[i] = TestItems.VALUES[random.nextInt(TestItems.VALUES.length)];
        return result;
    }

    private static List<?> contents(final Iterable<?> lookup) {
        final List<Object> result = new ArrayList<>();
        lookup.forEach(result::add);
        return result;
    }

    private static final class LookupPair {
        final Object baseline;
        final Object optimized;
        final Method one;
        final Method two;
        final Method three;
        final Method unorderedTwo;
        final Method unorderedThree;
        final Method queryOne;
        final Method queryTwo;
        final Method queryThree;
        final Method queryNormalized;

        LookupPair(final ClassLoader loader) throws Exception {
            final Class<?> original = loader.loadClass("crazypants.enderio.base.recipe.lookup.TriItemLookup");
            final Class<?> replacement = loader.loadClass(UnorderedAlloyRecipeLookup.class.getName());
            baseline = original.getConstructor().newInstance();
            optimized = replacement.getConstructor().newInstance();
            one = original.getMethod("addRecipe", Object.class, Item.class);
            two = original.getMethod("addRecipe", Object.class, Item.class, Item.class);
            three = original.getMethod("addRecipe", Object.class, Item.class, Item.class, Item.class);
            unorderedTwo = replacement.getMethod("addUnordered", Object.class, Item.class, Item.class);
            unorderedThree = replacement.getMethod("addUnordered", Object.class, Item.class, Item.class, Item.class);
            queryOne = original.getMethod("getRecipes", Item.class);
            queryTwo = original.getMethod("getRecipes", Item.class, Item.class);
            queryThree = original.getMethod("getRecipes", Item.class, Item.class, Item.class);
            queryNormalized = original.getMethod("getRecipes3", Item.class, Item.class, Item.class);
        }

        void add(final Object recipe, final Item[] inputs) throws Exception {
            final Item a = inputs[0];
            if (inputs.length == 1) {
                one.invoke(baseline, recipe, a);
                one.invoke(optimized, recipe, a);
            } else if (inputs.length == 2) {
                final Item b = inputs[1];
                two.invoke(baseline, recipe, a, b);
                two.invoke(baseline, recipe, b, a);
                unorderedTwo.invoke(optimized, recipe, a, b);
            } else {
                final Item b = inputs[1];
                final Item c = inputs[2];
                for (final Item[] permutation : new Item[][]{{a, b, c}, {a, c, b}, {b, a, c},
                    {b, c, a}, {c, a, b}, {c, b, a}}) {
                    three.invoke(baseline, recipe, permutation[0], permutation[1], permutation[2]);
                }
                unorderedThree.invoke(optimized, recipe, a, b, c);
            }
        }

        void assertEquivalent(final String stage) throws Exception {
            for (final Item x : TestItems.VALUES) {
                assertEquals(queryOne.invoke(baseline, x), queryOne.invoke(optimized, x), stage + " / one input");
                for (final Item y : TestItems.VALUES) {
                    assertEquals(queryTwo.invoke(baseline, x, y), queryTwo.invoke(optimized, x, y), stage + " / two inputs");
                    for (final Item z : TestItems.VALUES) {
                        assertEquals(queryThree.invoke(baseline, x, y, z), queryThree.invoke(optimized, x, y, z),
                            stage + " / three inputs");
                        assertEquals(queryNormalized.invoke(baseline, x, y, z), queryNormalized.invoke(optimized, x, y, z),
                            stage + " / AIR-normalized inputs");
                    }
                }
            }
            assertEquals(contents((Iterable<?>) baseline), contents((Iterable<?>) optimized), stage + " / iteration");
        }
    }

    private static ClassLoader lookupLoader() {
        final String fixture = TestItems.class.getName().replace('.', '/');
        final Remapper sentinel = new Remapper() {
            @Override
            public String map(final String name) {
                return name.equals("net/minecraft/init/Items") ? fixture : name;
            }

            @Override
            public String mapFieldName(final String owner, final String name, final String descriptor) {
                return owner.equals("net/minecraft/init/Items") && name.equals("field_190931_a") ? "AIR" : name;
            }
        };
        return new ClassLoader(AlloyLookupTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith("crazypants.enderio.base.recipe.lookup.")
                    && !name.equals(UnorderedAlloyRecipeLookup.class.getName())) {
                    return super.loadClass(name, resolve);
                }
                Class<?> type = findLoadedClass(name);
                if (type == null) {
                    try (final InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        if (in == null) throw new ClassNotFoundException(name);
                        final ClassWriter out = new ClassWriter(0);
                        final ClassVisitor remapped = new ClassRemapper(out, sentinel);
                        final ClassVisitor fixtureIds = new ClassVisitor(Opcodes.ASM5, remapped) {
                            @Override
                            public MethodVisitor visitMethod(final int access, final String methodName,
                                                             final String descriptor, final String signature,
                                                             final String[] exceptions) {
                                return new MethodVisitor(Opcodes.ASM5,
                                    super.visitMethod(access, methodName, descriptor, signature, exceptions)) {
                                    @Override
                                    public void visitMethodInsn(final int opcode, final String owner, final String calledName,
                                                                final String calledDescriptor, final boolean isInterface) {
                                        if (opcode == Opcodes.INVOKESTATIC && owner.equals("net/minecraft/item/Item")
                                            && (calledName.equals("getIdFromItem") || calledName.equals("func_150891_b"))
                                            && calledDescriptor.equals("(Lnet/minecraft/item/Item;)I")) {
                                            super.visitMethodInsn(opcode, fixture, "id", calledDescriptor, false);
                                        } else {
                                            super.visitMethodInsn(opcode, owner, calledName, calledDescriptor, isInterface);
                                        }
                                    }
                                };
                            }
                        };
                        new ClassReader(in).accept(fixtureIds, 0);
                        final byte[] bytes = out.toByteArray();
                        type = defineClass(name, bytes, 0, bytes.length);
                    } catch (final Exception failure) {
                        throw new ClassNotFoundException(name, failure);
                    }
                }
                if (resolve) resolveClass(type);
                return type;
            }
        };
    }
}
