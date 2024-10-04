package github.kasuminova.stellarcore.common.util;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class ClassUtils {

    private static final Map<Class<?>, Set<Class<?>>> CLASS_INTERFACES_CACHE = new WeakHashMap<>();

    private static final Map<Class<?>, Set<Class<?>>> CLASS_SUPERCLASSES_CACHE = new WeakHashMap<>();

    private static final Map<Class<?>, Set<Class<?>>> SUBCLASS_CACHE = new WeakHashMap<>();

    public static Set<Class<?>> getAllInterfaces(Class<?> clazz) {
        return CLASS_INTERFACES_CACHE.computeIfAbsent(clazz, (key) -> getAllInterfaces(clazz, new ReferenceOpenHashSet<>()));
    }

    public static Set<Class<?>> getAllSuperClasses(Class<?> clazz, @Nullable Class<?> topClass) {
        return CLASS_SUPERCLASSES_CACHE.computeIfAbsent(clazz, (key) -> getAllSuperClasses(clazz, topClass, new ReferenceOpenHashSet<>()));
    }

    protected static Set<Class<?>> getAllInterfaces(Class<?> clazz, Set<Class<?>> interfaceSet) {
        interfaceSet.addAll(Arrays.asList(clazz.getInterfaces()));
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            getAllInterfaces(superClass, interfaceSet);
        }
        return interfaceSet;
    }

    protected static Set<Class<?>> getAllSuperClasses(Class<?> clazz, @Nullable Class<?> topClass, Set<Class<?>> superClasses) {
        boolean topClassIsInterface = topClass != null && topClass.isInterface();

        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && (!topClassIsInterface || superClass != topClass)) {
            if (topClassIsInterface && getAllInterfaces(superClass).contains(topClass)) {
                return superClasses;
            }
            superClasses.add(superClass);
            getAllSuperClasses(superClass, topClass, superClasses);
        }
        return superClasses;
    }

}
