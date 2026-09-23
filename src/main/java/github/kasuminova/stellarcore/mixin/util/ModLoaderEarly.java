package github.kasuminova.stellarcore.mixin.util;

import github.kasuminova.stellarcore.mixin.StellarCoreEarlyMixinLoader;
import net.minecraftforge.fml.common.Loader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class ModLoaderEarly {

    private static final MethodHandle loadMod;

    static {
        Object o;
        MethodHandle load;
        try {
            var c = Class.forName("com.cleanroommc.discovery.CleanroomModDiscoverer");
            o = c.getMethod("instance").invoke(null);
            MethodType mt = MethodType.methodType(boolean.class, String.class);
            load = MethodHandles.lookup().bind(o,"isModPresent", mt);
        } catch (Exception e) {
            load = null;
        }
        loadMod = load;
    }

    public static boolean isModLoad(String modid) {
        if (loadMod != null) {
            try {
                return (boolean) loadMod.invoke(modid);
            } catch (Throwable ignored) {

            }
        }
        return Loader.isModLoaded(modid);
    }

    public static boolean isClassPresent(String className) {
        String classFilePath = className.replace('.', '/') + ".class";
        ClassLoader classLoader = StellarCoreEarlyMixinLoader.class.getClassLoader();
        return classLoader.getResource(classFilePath) != null;
    }

}
