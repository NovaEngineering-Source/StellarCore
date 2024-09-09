package github.kasuminova.stellarcore.client.hitokoto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.kasuminova.stellarcore.StellarCore;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import github.kasuminova.stellarcore.common.util.StellarLog;
import net.minecraft.util.JsonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HitokotoAPI {
    public static final String API_URL = "https://v1.hitokoto.cn/";
    public static final String INTERNATIONAL_API_URL = "https://international.v1.hitokoto.cn";

    public static String hitokotoCache = null;

    private static final Gson DESERIALIZER = new GsonBuilder()
            .registerTypeHierarchyAdapter(HitokotoResult.class, new HitokotoDeserializer())
            .create();

    public static String getHitokotoCache() {
        return hitokotoCache;
    }

    public static String getRandomHitokoto() {
        if (hitokotoCache != null) {
            return hitokotoCache;
        }

        String jsonStr;
        try {
            jsonStr = getStringFromURL(API_URL);
        } catch (IOException e) {
            try {
                jsonStr = getStringFromURL(INTERNATIONAL_API_URL);
            } catch (IOException ex) {
                if (StellarCoreConfig.DEBUG.enableDebugLog) {
                    StellarLog.LOG.warn("[DEBUG] Failed to fetch Hitokoto API.", e);
                    StellarLog.LOG.warn(ex);
                }
                return "";
            }
        }

        if (jsonStr == null || jsonStr.isEmpty()) {
            return "";
        }

        HitokotoResult hitokoto;
        try {
            hitokoto = JsonUtils.fromJson(DESERIALIZER, jsonStr, HitokotoResult.class, true);
        } catch (Exception e) {
            if (StellarCoreConfig.DEBUG.enableDebugLog) {
                StellarLog.LOG.warn("[DEBUG] Failed to fetch Hitokoto API.", e);
            }
            return "";
        }

        if (hitokoto == null) {
            return "";
        }

        String assembled = assembleHitokoto(hitokoto);
        if (!assembled.isEmpty()) {
            hitokotoCache = assembled;
        }
        return assembled;
    }

    public static String assembleHitokoto(HitokotoResult result) {
        String hitokoto = result.getHitokoto();
        String fromWho = result.getFromWho();
        if (fromWho.isEmpty()) {
            fromWho = result.getFrom();
            if (fromWho.isEmpty()) {
                fromWho = result.getCreator();
            }
        }

        if (hitokoto != null && fromWho != null) {
            return hitokoto + " —— " + fromWho;
        }

        return "";
    }

    public static String getStringFromURL(String urlStr) throws IOException {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));

            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            reader.close();
            connection.disconnect();
            return stringBuilder.toString();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
