package github.kasuminova.stellarcore.common.util;

@SuppressWarnings("unused")
public class RuntimeEnv {
    private RuntimeEnv() {
    }

    public static final boolean IS_CLEANROOM_LOADER = isClassInPath("com.cleanroommc.common.CleanroomContainer");

    public static boolean isClassInPath(String className) {
        String classPath = className.replace('.', '/') + ".class";
        try {
            return Thread.currentThread().getContextClassLoader().getResource(classPath) != null;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean isClassLoaded(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
