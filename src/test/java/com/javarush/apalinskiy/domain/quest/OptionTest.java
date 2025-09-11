package com.javarush.apalinskiy.domain.quest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Option")
class OptionTest {

    @Nested
    @DisplayName("constructor")
    class Ctor {

        @Test
        @DisplayName("sets fields when valid then ok")
        void setsFields() {
            // Given
            String choice = "Open";
            Integer next = 2;
            // When
            Option o = new Option(choice, next);
            // Then
            assertEquals("Open", o.getChoice());
            assertEquals(2, o.getNext());
        }

        @Test
        @DisplayName("allows next=null when constructed then ok")
        void allowsNullNext() {
            // Given
            String choice = "ghost";
            // When
            Option o = new Option(choice, null);
            // Then
            assertNull(o.getNext());
        }

        @Test
        @DisplayName("throws NPE when choice=null then error")
        void throwsOnNullChoice() {
            // Given / When / Then
            assertThrows(NullPointerException.class, () -> new Option(null, 1));
        }
    }

    @Nested
    @DisplayName("normalizedChoice()")
    class Normalized {

        @Test
        @DisplayName("lowercases and collapses spaces when mixed case/whitespace then normalized")
        void normalizesWhitespaceAndCase() {
            // Given
            Option o = new Option("  HeLLo \t WoRLD  ", 3);
            // When
            String norm = o.normalizedChoice();
            // Then
            assertEquals("hello world", norm);
        }

        @Test
        @DisplayName("returns empty when choice only whitespace then empty")
        void returnsEmptyForWhitespaceOnly() {
            // Given
            Option o = new Option(" \n\t ", 1);
            // When
            String norm = o.normalizedChoice();
            // Then
            assertEquals("", norm);
        }
    }

    @Nested
    @DisplayName("record semantics")
    class RecordSemantics {

        @Test
        @DisplayName("equals/hashCode when same components then equal")
        void equalsAndHashCode() {
            // Given
            Option a = new Option("go", 2);
            Option b = new Option("go", 2);
            // When
            boolean eq = a.equals(b);
            // Then
            assertTrue(eq);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("toString contains field names when called then readable")
        void toStringContainsFields() {
            // Given
            Option o = new Option("take key", 5);
            // When
            String s = o.toString();
            // Then
            assertTrue(s.contains("choice="));
            assertTrue(s.contains("next="));
        }
    }
}