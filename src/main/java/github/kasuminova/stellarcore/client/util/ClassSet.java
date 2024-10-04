package github.kasuminova.stellarcore.client.util;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.util.Optional;
import java.util.Set;

public abstract class ClassSet {

    protected final Set<Class<?>> classSet = new ReferenceOpenHashSet<>();

    public ClassSet() {
        reload();
    }

    public abstract void reload();

    public void add(Class<?> clazz) {
        classSet.add(clazz);
    }

    public boolean isInSet(Class<?> clazz) {
        return !classSet.isEmpty() && classSet.contains(clazz);
    }

    public static Optional<Class<?>> findClass(String name) {
        try {
            return Optional.of(Class.forName(name));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

}
