package github.kasuminova.stellarcore.client.util;

import github.kasuminova.stellarcore.StellarCore;
import github.kasuminova.stellarcore.client.hitokoto.HitokotoAPI;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import github.kasuminova.stellarcore.mixin.StellarCoreEarlyMixinLoader;
import org.lwjgl.opengl.Display;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class TitleUtils {
    public static final String VANILLA_TITLE = "Minecraft 1.12.2";

    public static String currentTitle = null;
    public static String lastCurrentTitle = null;
    public static boolean unsupportedPlatform = false;

    /**
     * 设置一言随机标题，必须在客户端主线程使用。
     * 如果一言缓存为空，则尝试重新获取一言。
     *
     * @param state 当前状态
     */
    public static void setRandomTitle(final String state) {
        if (!StellarCoreConfig.FEATURES.enableTitle) {
            return;
        }
        lastCurrentTitle = currentTitle;

        String hitokotoCache = HitokotoAPI.getHitokotoCache();
        if (hitokotoCache != null) {
            currentTitle = buildTitle(state, hitokotoCache);
            setTitle();
        } else {
            CompletableFuture.runAsync(HitokotoAPI::getRandomHitokoto);
            currentTitle = buildTitle(state, null);
            setTitle();
        }
    }

    /**
     * 设置一言随机标题，必须在客户端主线程使用。
     * 如果一言缓存为空，则尝试重新获取一言。
     */
    public static void setRandomTitle() {
        if (!StellarCoreConfig.FEATURES.enableTitle) {
            return;
        }
        lastCurrentTitle = currentTitle;

        String hitokotoCache = HitokotoAPI.getHitokotoCache();

        if (hitokotoCache != null) {
            currentTitle = buildTitle(null, hitokotoCache);
            setTitle();
        } else {
            currentTitle = buildTitle(null, null);
            setTitle();
        }
    }

    /**
     * 设置一言随机标题，可以在其他线程使用。
     *
     * @param state 当前状态
     */
    public static void setRandomTitleSync(String state) {
        if (!StellarCoreConfig.FEATURES.enableTitle) {
            return;
        }
        lastCurrentTitle = currentTitle;
        currentTitle = buildTitle(state, HitokotoAPI.getHitokotoCache());
    }

    /**
     * 设置一言随机标题，可以在其他线程使用。
     */
    public static void setRandomTitleSync() {
        if (!StellarCoreConfig.FEATURES.enableTitle) {
            return;
        }
        lastCurrentTitle = currentTitle;
        currentTitle = buildTitle(null, HitokotoAPI.getHitokotoCache());
    }

    public static String buildTitle(final String state, final String hitokoto) {
        String title = StellarCoreConfig.FEATURES.title;
        boolean useHitokoto = StellarCoreConfig.FEATURES.hitokoto;
        if (state == null) {
            if (!useHitokoto || hitokoto == null) {
                return title;
            }
            return String.format("%s | %s", title, hitokoto);
        }
        if (!useHitokoto || hitokoto == null) {
            return String.format("%s | %s", title, state);
        }

        return String.format("%s | %s | %s", title, state, hitokoto);
    }

    public static void checkTitleState() {
        if (!StellarCoreConfig.FEATURES.enableTitle) {
            return;
        }
        if (currentTitle == null) {
            return;
        }

        String title = Display.getTitle();
        if (!title.equals(currentTitle)) {
            setTitle();
        }
    }

    private static void setTitle() {
        if (StellarCoreEarlyMixinLoader.isCleanroomLoader() && !unsupportedPlatform) {
            try {
                Class<?> Display = Class.forName("org.lwjgl.opengl.Display");
                Method getWindow = Display.getDeclaredMethod("getWindow");
                long result = (long) getWindow.invoke(null);
                if (result != 0) {
                    Class<?> GLFW = Class.forName("org.lwjgl3.glfw.GLFW");
                    Method glfwSetWindowTitle = GLFW.getDeclaredMethod("glfwSetWindowTitle", long.class, CharSequence.class);
                    glfwSetWindowTitle.invoke(null, result, currentTitle);
                }
            } catch (Exception e) {
                StellarLog.LOG.warn("Failed to set CleanroomLoader title, maybe platform is unsupported.", e);
                unsupportedPlatform = true;
            }
            return;
        }
        Display.setTitle(currentTitle);
    }
}
