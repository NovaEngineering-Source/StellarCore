package github.kasuminova.stellarcore.mixin.util;

import alexiil.mc.mod.load.ClsManager;
import alexiil.mc.mod.load.render.MinecraftDisplayerRenderer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.Optional;

public class CustomLoadingScreenUtils {

    @Optional.Method(modid = "customloadingscreen")
    public static void cleanCLSTextures() {
        try {
            MinecraftDisplayerRenderer instance = ObfuscationReflectionHelper.getPrivateValue(ClsManager.class, null, "instance");
            ObfuscationReflectionHelper.setPrivateValue(MinecraftDisplayerRenderer.class, instance, null, "renderingParts");
        } catch (Exception ignored) {
        }
    }

}
