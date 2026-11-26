package github.kasuminova.stellarcore.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

public final class LanguageLineParser {
    private static final Pattern FORMATS = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");
    private static final byte[] PARSE_ESCAPES = "PARSE_ESCAPES".getBytes(StandardCharsets.US_ASCII);

    private LanguageLineParser() {
    }

    /**
     * Matches Forge's leading '#' and String.trim() rules on UTF-8 language files.
     */
    public static boolean usesPropertyEscapes(final byte[] text) {
        int lineStart = 0;
        for (int end = 0; end <= text.length; end++) {
            if (end < text.length && text[end] != '\r' && text[end] != '\n') continue;
            if (end - lineStart > PARSE_ESCAPES.length && text[lineStart] == '#') {
                int from = lineStart + 1;
                int to = end;
                // ASCII control characters and space decode to the same UTF-8 code points used by trim().
                while (from < to && Byte.toUnsignedInt(text[from]) <= ' ') from++;
                while (to > from && Byte.toUnsignedInt(text[to - 1]) <= ' ') to--;
                if (matchesDirective(text, from, to)) return true;
            }
            lineStart = end + 1;
        }
        return false;
    }

    private static boolean matchesDirective(final byte[] text, final int from, final int to) {
        if (to - from != PARSE_ESCAPES.length) return false;
        for (int i = 0; i < PARSE_ESCAPES.length; i++) {
            if (text[from + i] != PARSE_ESCAPES[i]) return false;
        }
        return true;
    }

    /** Reads completely before changing the map, without retaining a list or String for each line. */
    public static void read(final Map<String, String> properties, final InputStream stream) throws IOException {
        final Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        char[] text = new char[8192];
        int length = 0;
        int count;
        while ((count = reader.read(text, length, text.length - length)) != -1) {
            length += count;
            if (length == text.length) {
                text = Arrays.copyOf(text, Math.addExact(length, Math.max(length >> 1, 8192)));
            }
        }
        // The caller owns the stream. Match BufferedReader.readLine's CR, LF and CRLF handling.
        int start = 0;
        for (int end = 0; end < length; end++) {
            if (text[end] == '\r' || text[end] == '\n') {
                put(properties, text, start, end);
                if (text[end] == '\r' && end + 1 < length && text[end + 1] == '\n') end++;
                start = end + 1;
            }
        }
        put(properties, text, start, length);
    }

    private static void put(final Map<String, String> properties, final char[] text, final int start, final int end) {
        if (start == end || text[start] == '#') return;
        for (int separator = start; separator < end; separator++) {
            if (text[separator] == '=') {
                final String value = new String(text, separator + 1, end - separator - 1);
                properties.put(new String(text, start, separator - start), normalize(value));
                return;
            }
        }
    }

    public static void put(final Map<String, String> properties, final String line) {
        if (line.isEmpty() || line.charAt(0) == '#') {
            return;
        }
        final int separator = line.indexOf('=');
        if (separator < 0) {
            return;
        }
        final String key = line.substring(0, separator);
        final String value = line.substring(separator + 1);
        properties.put(key, normalize(value));
    }

    private static String normalize(final String value) {
        return value.indexOf('%') < 0 ? value : FORMATS.matcher(value).replaceAll("%$1s");
    }
}
