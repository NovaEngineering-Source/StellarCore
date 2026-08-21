package github.kasuminova.stellarcore.mixin.util;

import github.kasuminova.stellarcore.mixin.StellarCoreEarlyMixinLoader;
import net.minecraftforge.fml.common.Loader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class ModLoaderEarly {

    private static final MethodHandle loadMod;
    private static final Object obj;

    static {
        Object o;
        MethodHandle load;
        try {
            var c = Class.forName("com.cleanroommc.discovery.CleanroomModDiscoverer");
            o = c.getMethod("instance").invoke(null);
            MethodType mt = MethodType.methodType(boolean.class, String.class);
            load = MethodHandles.lookup().findVirtual(c, "isModPresent", mt);
        } catch (Exception e) {
            load = null;
            o = null;
        }
        obj = o;
        loadMod = load;
    }

    public static boolean isModLoad(String modid) {
        if (loadMod != null) {
            try {
                return (boolean) loadMod.invoke(obj,modid);
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
