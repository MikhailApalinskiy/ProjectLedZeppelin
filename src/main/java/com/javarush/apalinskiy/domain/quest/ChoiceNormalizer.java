package com.javarush.apalinskiy.domain.quest;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ChoiceNormalizer {
    private static final Pattern WS = Pattern.compile("\\s+");

    private ChoiceNormalizer() {}

    public static String normalize(String s) {
        if (s == null) return "";
        return WS.matcher(s).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
    }
}
