package com.javarush.apalinskiy.domain.quest.choice;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class for normalizing player choice input in quests.
 *
 * <p>This class provides a single static method {@link #normalize(String)}
 * that trims, lowercases, and collapses multiple whitespace characters into a single space.
 * It ensures consistent text comparison between user input and stored quest options.</p>
 *
 * <p>This class is immutable and cannot be instantiated.</p>
 */
public final class ChoiceNormalizer {

    /**
     * Precompiled pattern that matches one or more whitespace characters.
     */
    private static final Pattern WS = Pattern.compile("\\s+");

    private ChoiceNormalizer() {
    }

    /**
     * Normalizes a given string by:
     * <ul>
     *   <li>replacing multiple spaces or whitespace sequences with a single space,</li>
     *   <li>trimming leading and trailing spaces,</li>
     *   <li>and converting all characters to lowercase using {@link Locale#ROOT}.</li>
     * </ul>
     *
     * @param s the raw input string (may be {@code null})
     * @return a normalized, lowercased, and trimmed string;
     * returns an empty string if {@code s} is {@code null}
     */
    public static String normalize(String s) {
        if (s == null) return "";
        return WS.matcher(s).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
    }
}
