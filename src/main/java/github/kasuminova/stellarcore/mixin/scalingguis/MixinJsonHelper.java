package github.kasuminova.stellarcore.mixin.scalingguis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import spazley.scalingguis.ScalingGUIs;
import spazley.scalingguis.config.JsonHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(JsonHelper.class)
public class MixinJsonHelper {

    @Shadow(remap = false) private static Gson GSON;

    /**
     * @author Kasumi_Nova
     * @reason 在迭代器里面用 {@link List#remove(Object)} 是不行的捏。
     */
    @Inject(method = "getKeyList", at = @At("HEAD"), cancellable = true, remap = false)
    @SuppressWarnings("unchecked")
    private static void getKeyList(final JsonObject jsonObjectIn, final CallbackInfoReturnable<List<String>> cir) {
        if (!StellarCoreConfig.BUG_FIXES.scalingGuis.fixJsonHelper) {
            return;
        }

        Map<String, Object> map = (Map<String, Object>) GSON.fromJson(jsonObjectIn, LinkedHashMap.class);
        List<String> keyList = new ArrayList<>(map.keySet());
        keyList.removeIf(s -> !isClassExists(s));
        cir.setReturnValue(keyList);
    }

    @Unique
    @SuppressWarnings("AddedMixinMembersNamePattern")
    private static boolean isClassExists(final String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Exception var6) {
            ScalingGUIs.logger.error("Unknown class '" + className + "'. Removing from check list. Will be left in json assets.scalingguis.file.", var6);
            return false;
        }
    }

}
