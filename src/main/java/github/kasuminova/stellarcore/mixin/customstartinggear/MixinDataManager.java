package github.kasuminova.stellarcore.mixin.customstartinggear;

import com.brandon3055.csg.DataManager;
import com.brandon3055.csg.LogHelper;
import com.brandon3055.csg.lib.PlayerSlot;
import com.brandon3055.csg.lib.StackReference;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.UTF8FileReader;
import github.kasuminova.stellarcore.common.util.UTF8FileWriter;
import org.apache.commons.compress.utils.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"StaticVariableMayNotBeInitialized", "StaticVariableUsedBeforeInitialization"})
@Mixin(DataManager.class)
public class MixinDataManager {

    @Shadow(remap = false)
    public static Map<PlayerSlot, StackReference> spawnInventory;

    @Shadow(remap = false)
    public static Map<String, Map<PlayerSlot, StackReference>> kits;

    @Shadow(remap = false)
    private static File configFile;

    /**
     * @author Kasumi_Nova
     * @reason Use UTF-8 Encoder
     */
    @Inject(method = "saveConfig", at = @At("HEAD"), cancellable = true, remap = false)
    private static void saveConfig(final CallbackInfo ci) throws IOException {
        if (!StellarCoreConfig.BUG_FIXES.customStartingGear.dataManager) {
            return;
        }
        ci.cancel();
        
        if (spawnInventory == null) {
            LogHelper.error("Could not save config because inventory array was null!");
            return;
        }

        JsonObject obj = new JsonObject();
        JsonObject inv = new JsonObject();

        spawnInventory.forEach((playerSlot, stackReference) -> inv.addProperty(playerSlot.toString(), stackReference.toString()));

        obj.add("inventory", inv);

        if (!kits.isEmpty()) {
            JsonObject kitsObj = new JsonObject();

            kits.forEach((name, kit) -> {
                JsonObject kitObj = new JsonObject();
                kit.forEach((playerSlot, stackReference) -> kitObj.addProperty(playerSlot.toString(), stackReference.toString()));
                kitsObj.add(name, kitObj);
            });

            obj.add("kits", kitsObj);
        }

        JsonWriter writer = new JsonWriter(new UTF8FileWriter(configFile));
        writer.setIndent("  ");
        Streams.write(obj, writer);
        writer.flush();
        IOUtils.closeQuietly(writer);
    }

    /**
     * @author Kasumi_Nova
     * @reason Use UTF-8 Encoder
     */
    @Inject(method = "loadConfig", at = @At("HEAD"), cancellable = true, remap = false)
    private static void loadConfig(final CallbackInfo ci) throws IOException {
        if (!StellarCoreConfig.BUG_FIXES.customStartingGear.dataManager) {
            return;
        }
        ci.cancel();

        if (!configFile.exists()) {
            spawnInventory = null;
            LogHelper.warn("Custom Spawn Inventory has not been configured yet! Canceling config load!");
            return;
        }

        JsonObject obj;
        JsonParser parser = new JsonParser();
        UTF8FileReader reader = new UTF8FileReader(configFile);
        JsonElement element = parser.parse(reader);
        IOUtils.closeQuietly(reader);

        if (!element.isJsonObject()) {
            LogHelper.warn("Detected invalid config json! Canceling config load!");
            return;
        }

        obj = element.getAsJsonObject();

        if (obj.has("inventory") && obj.get("inventory").isJsonObject()) {
            LogHelper.info("Reading starting inventory config");
            spawnInventory = new HashMap<>();
            JsonObject inv = obj.get("inventory").getAsJsonObject();
            inv.entrySet().forEach(entry -> {
                PlayerSlot slot = PlayerSlot.fromString(entry.getKey());
                StackReference stack = StackReference.fromString(entry.getValue().getAsJsonPrimitive().getAsString());
                spawnInventory.put(slot, stack);
            });
            LogHelper.info("Loaded " + spawnInventory.size() + " starting items.");
        }

        if (obj.has("kits") && obj.get("kits").isJsonObject()) {
            LogHelper.info("Reading kits from config");
            JsonObject kits = obj.get("kits").getAsJsonObject();
            kits.entrySet().forEach(entry -> {
                String name = entry.getKey();
                JsonObject items = entry.getValue().getAsJsonObject();
                Map<PlayerSlot, StackReference> kitMap = DataManager.kits.computeIfAbsent(name, s -> new HashMap<>());

                items.entrySet().forEach(kitEntry -> {
                    PlayerSlot slot = PlayerSlot.fromString(kitEntry.getKey());
                    StackReference stack = StackReference.fromString(kitEntry.getValue().getAsJsonPrimitive().getAsString());
                    kitMap.put(slot, stack);
                });

                LogHelper.info("Loaded " + kitMap.size() + " items for kit " + name);
            });
        }
    }

}
