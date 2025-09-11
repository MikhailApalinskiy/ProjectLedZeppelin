package com.javarush.apalinskiy.domain.quest.choice;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

@DisplayName("ChoiceNormalizer")
class ChoiceNormalizerTest {

    @Test
    @DisplayName("has private ctor")
    void hasPrivateCtor() throws Exception {
        // Given / When
        Constructor<?> c = ChoiceNormalizer.class.getDeclaredConstructor();
        // Then
        assertTrue(Modifier.isPrivate(c.getModifiers()));
    }

    @Nested
    @DisplayName("normalize()")
    class Normalize {

        @Test
        @DisplayName("returns empty string when input is null")
        void returnsEmptyOnNull() {
            // Given
            // When
            String result = ChoiceNormalizer.normalize(null);
            // Then
            assertEquals("", result);
        }

        @Test
        @DisplayName("trims leading/trailing whitespace when given spaced string then no spaces")
        void trimsWhitespace() {
            // Given
            String input = "   hello   ";
            // When
            String result = ChoiceNormalizer.normalize(input);
            // Then
            assertEquals("hello", result);
        }

        @Test
        @DisplayName("collapses multiple spaces when given spaced words then single space")
        void collapsesMultipleSpaces() {
            // Given
            String input = "a    b\tc";
            // When
            String result = ChoiceNormalizer.normalize(input);
            // Then
            assertEquals("a b c", result);
        }

        @Test
        @DisplayName("converts to lowercase when given mixed case then lowercased")
        void convertsToLowercase() {
            // Given
            String input = "HeLLo WoRLD";
            // When
            String result = ChoiceNormalizer.normalize(input);
            // Then
            assertEquals("hello world", result);
        }

        @Test
        @DisplayName("handles tabs and newlines when given mixed whitespace then normalized")
        void handlesTabsAndNewlines() {
            // Given
            String input = "line1\tline2\nline3";
            // When
            String result = ChoiceNormalizer.normalize(input);
            // Then
            assertEquals("line1 line2 line3", result);
        }

        @Test
        @DisplayName("keeps inner single spaces when already normalized")
        void keepsSingleSpaces() {
            // Given
            String input = "one two three";
            // When
            String result = ChoiceNormalizer.normalize(input);
            // Then
            assertEquals("one two three", result);
        }
    }
}