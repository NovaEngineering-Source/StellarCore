package github.kasuminova.stellarcore.client.gui.font;

import com.fred.jianghun.truergb.Colors;
import com.fred.jianghun.truergb.RGBSettings;
import com.fred.jianghun.truergb.Utils;
import github.kasuminova.stellarcore.mixin.rgbchat.MixinRGBSettings;
import net.minecraft.util.Tuple;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class RGBSettingsUtils {

    public static List<Tuple<String, String>> splitRGBAndContents(String str) {
        if (str != null && !str.isEmpty()) {
            List<Tuple<String, String>> result = new ArrayList<>();
            Matcher matcher = RGBSettings.PATTERN.matcher(str);

            int index = 0;
            RGBSettings lastSettings = RGBSettings.EMPTY;

            while (matcher.find(index)) {
                String subString = str.substring(index, matcher.start());
                if (!subString.isEmpty()) {
                    lastSettings.addLength(subString.length());
                    result.add(new Tuple<>(serializeRGBSettings(lastSettings), subString));
                }

                String format = matcher.group();
                if (format.startsWith("#")) {
                    String fString = matcher.group("rgb");
                    lastSettings = new RGBSettings(Arrays.stream(fString.split("-")).map(Colors::of).collect(Collectors.toList()));
                } else if (format.startsWith("§")) {
                    TextFormatting formatting = Utils.formattingOf(format.charAt(1));
                    if (formatting == null) {
                        throw new NullPointerException("Format: " + format);
                    }
                    lastSettings = lastSettings.withFormat(formatting);
                } else {
                    throw new IllegalStateException("Format: " + format);
                }

                index = matcher.end();
            }

            String remainingString = str.substring(index);
            lastSettings.addLength(remainingString.length());
            result.add(new Tuple<>(serializeRGBSettings(lastSettings), remainingString));

            return result;
        } else {
            return Collections.singletonList(new Tuple<>("", ""));
        }
    }

    public static String serializeRGBSettings(RGBSettings rgbSettings) {
        StringBuilder result = new StringBuilder();

        // Serialize colors
        List<String> colorStrings = ((MixinRGBSettings) rgbSettings).getColors().stream()
                .map(color -> String.format("%06X", color.toInt() & 0xFFFFFF))
                .collect(Collectors.toList());
        if (!colorStrings.isEmpty()) {
            result.append('#').append(String.join("-", colorStrings));
        }

        // Serialize control codes
        Boolean bold = rgbSettings.getBold();
        if (bold != null && bold) {
            result.append("§l");
        }
        Boolean italic = rgbSettings.getItalic();
        if (italic != null && italic) {
            result.append("§o");
        }
        Boolean underlined = rgbSettings.getUnderlined();
        if (underlined != null && underlined) {
            result.append("§n");
        }
        Boolean strikethrough = rgbSettings.getStrikethrough();
        if (strikethrough != null && strikethrough) {
            result.append("§m");
        }
        Boolean obfuscated = rgbSettings.getObfuscated();
        if (obfuscated != null && obfuscated) {
            result.append("§k");
        }

        return result.toString();
    }

}
