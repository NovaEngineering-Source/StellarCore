package github.kasuminova.stellarcore.client.util;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.util.Optional;
import java.util.Set;

public abstract class ClassBlackList {


    protected final Set<Class<?>> blackList = new ReferenceOpenHashSet<>();

    public ClassBlackList() {
        reload();
    }

    public abstract void reload();

    public void add(Class<?> clazz) {
        blackList.add(clazz);
    }

    public boolean isInBlackList(Class<?> clazz) {
        return blackList.contains(clazz);
    }

    public static Optional<Class<?>> findClass(String name) {
        try {
            return Optional.of(Class.forName(name));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

}
