package com.javarush.apalinskiy.quest.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class OptionTest {
    private Option option;

    @BeforeEach
    void setUp() {
        option = new Option("abc", 1);

    }

    @Nested
    class ConstructorValidation {

        @Test
        void choiceIsNullThrowsNullPointerExceptionTest() {
            // Given
            // When
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new Option(null, 1));
            // Then
            assertEquals("choice", ex.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", " \t", "\n"})
        void choiceIsBlankOrEmptyDoesNotThrowNullPointerExceptionTest(String input) {
            // Given
            // When / Then
            assertDoesNotThrow(() -> new Option(input, 1));
        }

        @Test
        void nextCanBeNullTest() {
            // Given
            // When / Then
            assertDoesNotThrow(() -> new Option("", null));
        }
    }

    @Nested
    class Accessors {

        @Test
        void choiceReturnTest() {
            // Given
            // When
            String choice = option.choice();
            // Then
            assertEquals("abc", choice);
        }

        @Test
        void nextReturnTest() {
            // Given
            // When
            Integer next = option.next();
            // Then
            assertEquals(Integer.valueOf(1), next);
        }
    }

    @Nested
    class NormalizedChoice {

        @ParameterizedTest
        @ValueSource(strings = {"a b", "a\tb", "a\n\t b"})
        void normalizeCompressWhitespaceTest(String input) {
            // Given
            Option option = new Option(input, 1);
            // When
            String result = option.normalizedChoice();
            // Then
            assertEquals("a b", result);
        }

        @ParameterizedTest
        @CsvSource({
                "'\t hello \n','hello'",
                "' hi','hi'",
                "'bye ','bye'"
        })
        void normalizeTrimTest(String input, String expected) {
            // Given
            Option option = new Option(input, 1);
            // When
            String result = option.normalizedChoice();
            // Then
            assertEquals(expected, result);
        }

        @Test
        void normalizeDoALowerCaseTest() {
            // Given
            Option option = new Option("MiXeD CaSe ÄÖÜ", 1);
            // When
            String result = option.normalizedChoice();
            // Then
            assertEquals("mixed case äöü", result);
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "\t\n"})
        void normalizeReturnBlankStringTest(String input) {
            // Given
            Option option = new Option(input, 1);
            // When
            String result = option.normalizedChoice();
            // Then
            assertEquals("", result);
        }

        @Test
        void normalizedChoiceIsIdempotentTest() {
            // Given
            Option option = new Option("already normalized", 7);
            // When
            String result = option.normalizedChoice();
            // Then
            assertEquals("already normalized", result);
        }

        @Test
        void normalizedChoiceDoesNotMutateOriginalChoiceTest() {
            // Given
            Option option = new Option(" HeLLo  WoRLd ", 1);
            // When
            String normalized = option.normalizedChoice();
            // Then
            assertEquals("hello world", normalized);
            assertEquals(" HeLLo  WoRLd ", option.choice());
        }

        @Test
        void normalizedChoiceIsLocaleIndependentTest() {
            // Given
            Locale prev = Locale.getDefault();
            try {
                Locale.setDefault(Locale.of("tr", "TR"));
                Option option = new Option("İSTANBUL", 1);
                // When
                String normalized = option.normalizedChoice();
                // Then
                assertEquals("i̇stanbul", normalized);
            } finally {
                Locale.setDefault(prev);
            }
        }
    }

    @Nested
    class JacksonDeserialization {

        private final ObjectMapper mapper = new ObjectMapper();

        @Test
        void jacksonDeserializeOkTest() throws Exception {
            // Given
            String json = "{\"choice\":\" Go \\t NORTH \",\"next\":3}";
            // When
            Option o = mapper.readValue(json, Option.class);
            // Then
            assertEquals(" Go \t NORTH ", o.choice());
            assertEquals(3, o.next());
            assertEquals("go north", o.normalizedChoice());
        }

        @Test
        void jacksonDeserializeWithoutNextTest() throws Exception {
            // Given
            String json = "{\"choice\":\"Take torch\"}";
            // When
            Option o = mapper.readValue(json, Option.class);
            // Then
            assertEquals("Take torch", o.choice());
            assertNull(o.next());
        }

        @Test
        void jacksonDeserializeWithoutChoiceThrowsTest() {
            // Given
            String json = "{\"next\":1}";
            // When
            Exception ex = assertThrows(JsonProcessingException.class,
                    () -> mapper.readValue(json, Option.class));
            Throwable cause = ex.getCause();
            while (cause != null && cause.getCause() != null) {
                cause = cause.getCause();
            }
            // Then
            assertNotNull(cause);
            assertInstanceOf(NullPointerException.class, cause);
            assertEquals("choice", cause.getMessage());
        }

        @Test
        void jacksonDeserializeIgnoresUnknownPropertiesTest() throws Exception {
            // Given
            String json = "{\"choice\":\"Open door\",\"next\":2,\"unexpected\":\"xxx\"}";
            // When
            Option o = mapper.readValue(json, Option.class);
            // Then
            assertEquals("Open door", o.choice());
            assertEquals(2, o.next());
        }
    }

    @Nested
    class EqualityAndToString {

        @Test
        void equalsSameValuesTest() {
            // Given
            Option a = new Option("x", 10);
            Option b = new Option("x", 10);
            // When / Then
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void notEqualsDifferentValuesTest() {
            // Given
            Option a = new Option("x", 10);
            Option b = new Option("x", 11);
            Option c = new Option("y", 10);
            // When / Then
            assertNotEquals(a, b);
            assertNotEquals(a, c);
        }

        @Test
        void toStringContainsComponentNamesTest() {
            // Given
            Option o = new Option("foo", 42);
            // When
            String s = o.toString();
            // Then
            assertTrue(s.contains("choice="));
            assertTrue(s.contains("next="));
            assertTrue(s.contains("foo"));
            assertTrue(s.contains("42"));
        }
    }
}