package github.kasuminova.stellarcore.client.integration.libnine;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Reads LibNine's type without constructing the model's JSON object tree.
 */
public final class L9ModelTypeReader {

    private L9ModelTypeReader() {
    }

    @Nullable
    public static String read(final JsonReader reader) throws IOException {
        reader.setLenient(true);
        reader.beginObject();
        String type = null;
        while (reader.hasNext()) {
            if ("9s".equals(reader.nextName())) {
                type = readString(reader);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        reader.setLenient(false);
        if (reader.peek() != JsonToken.END_DOCUMENT) {
            throw new IOException("Model JSON has trailing content");
        }
        return type;
    }

    @Nullable
    private static String readString(final JsonReader reader) throws IOException {
        final JsonToken token = reader.peek();
        if (token == JsonToken.STRING || token == JsonToken.NUMBER) {
            return reader.nextString();
        }
        if (token == JsonToken.BOOLEAN) {
            return Boolean.toString(reader.nextBoolean());
        }
        if (token == JsonToken.BEGIN_ARRAY) {
            reader.beginArray();
            String value = reader.hasNext() ? readString(reader) : null;
            while (reader.hasNext()) {
                reader.skipValue();
                value = null;
            }
            reader.endArray();
            return value;
        }
        reader.skipValue();
        return null;
    }
}
