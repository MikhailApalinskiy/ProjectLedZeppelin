package com.javarush.apalinskiy.domain.quest.choice;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class for normalizing user-provided choice text.
 * <p>
 * The normalization performs the following steps:
 * <ul>
 *   <li>Replaces any sequence of whitespace characters with a single space.</li>
 *   <li>Trims leading and trailing whitespace.</li>
 *   <li>Converts the string to lowercase using {@link Locale#ROOT}.</li>
 * </ul>
 *
 * <p>This helps to ensure consistent comparison of answers regardless of
 * user input formatting.</p>
 *
 * <p>The class is {@code final} and cannot be instantiated.</p>
 */
public final class ChoiceNormalizer {

    /**
     * Regular expression pattern that matches one or more whitespace characters.
     */
    private static final Pattern WS = Pattern.compile("\\s+");

    private ChoiceNormalizer() {
    }

    /**
     * Normalizes the given string for reliable comparison.
     *
     * @param s the input string (may be {@code null})
     * @return a normalized version of the string:
     * empty string if {@code null}, otherwise lowercased,
     * trimmed, and with collapsed whitespace
     */
    public static String normalize(String s) {
        if (s == null) return "";
        return WS.matcher(s).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
    }
}
