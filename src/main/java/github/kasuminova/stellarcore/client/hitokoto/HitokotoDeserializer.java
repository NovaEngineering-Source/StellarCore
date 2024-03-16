package github.kasuminova.stellarcore.client.hitokoto;

import com.google.gson.*;

import java.lang.reflect.Type;

public class HitokotoDeserializer implements JsonDeserializer<HitokotoResult> {

    @Override
    public HitokotoResult deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();
        HitokotoResult result = new HitokotoResult();

        result.setId(getJsonNodeInt(root, "id"))
                .setUUID(getJsonNodeString(root, "uuid"))
                .setHitokoto(getJsonNodeString(root, "hitokoto"))
                .setType(getJsonNodeString(root, "type"))
                .setFrom(getJsonNodeString(root, "from"))
                .setFromWho(getJsonNodeString(root, "from_who"))
                .setCreator(getJsonNodeString(root, "creator"))
                .setCreatorUid(getJsonNodeInt(root, "creator_uid"))
                .setReviewer(getJsonNodeInt(root, "reviewer"))
                .setCommitFrom(getJsonNodeString(root, "commit_from"))
                .setCreatedAt(getJsonNodeString(root, "created_at"))
                .setLength(getJsonNodeInt(root, "length"));

        return result;
    }

    private static int getJsonNodeInt(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            JsonElement element = json.get(memberName);
            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    return primitive.getAsInt();
                }
            }
        }
        return -1;
    }

    private static String getJsonNodeString(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            JsonElement element = json.get(memberName);
            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isString()) {
                    return primitive.getAsString();
                }
            }
        }
        return "";
    }
}
