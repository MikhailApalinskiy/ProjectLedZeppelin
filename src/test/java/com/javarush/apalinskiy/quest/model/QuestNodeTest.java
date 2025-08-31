package com.javarush.apalinskiy.quest.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestNodeTest {
    private Option op1;
    private Option op2;

    @BeforeEach
    void setUp() {
        op1 = new Option("Go North", 2);
        op2 = new Option("Go South", 3);
    }

    @Nested
    class ConstructorAndFactories {

        @Test
        void textNullThrowsNpeTest() {
            // Given
            int id = 1;
            // When
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> QuestNode.of(id, null, List.of(), false, null));
            // Then
            assertEquals("text", ex.getMessage());
        }

        @Test
        void idLeZeroThrowsIaeTest() {
            // Given
            int badId = 0;
            // When
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(badId, "text", List.of(), false, null));
            // Then
            assertEquals("id must be > 0", ex.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "\t", " \n "})
        void textBlankThrowsIaeTest(String blank) {
            // Given
            int id = 1;
            // When
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(id, blank, List.of(), false, null));
            // Then
            assertEquals("text must not be blank", ex.getMessage());
        }

        @Test
        void optionsNullBecomesEmptyTest() {
            // Given
            // When
            QuestNode n = QuestNode.of(1, "abc", null, false, null);
            // Then
            assertNotNull(n.getOptions());
            assertTrue(n.getOptions().isEmpty());
        }

        @Test
        void ofFactoryBuildsProperNodeTest() {
            // Given
            List<Option> options = List.of(op1, op2);
            // When
            QuestNode n = QuestNode.of(10, "hello", options, false, "/img.png");
            // Then
            assertEquals(10, n.getId());
            assertEquals("hello", n.getText());
            assertEquals(options, n.getOptions());
            assertFalse(n.isFin());
            assertEquals("/img.png", n.getImage());
        }

        @Test
        void finFactoryBuildsFinalNodeTest() {
            // Given
            int id = 7;
            // When
            QuestNode n = QuestNode.fin(id, "bye", "/x.jpg");
            // Then
            assertEquals(7, n.getId());
            assertEquals("bye", n.getText());
            assertTrue(n.isFin());
            assertTrue(n.getOptions().isEmpty());
            assertEquals("/x.jpg", n.getImage());
        }
    }

    @Nested
    class OptionsValidation {

        @Test
        void finalNodeWithOptionsThrowsTest() {
            // Given
            List<Option> options = List.of(op1);
            // When
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(1, "end", options, true, null));
            // Then
            assertEquals("final node should not have options", ex.getMessage());
        }

        @Test
        void duplicateNormalizedChoicesThrowTest() {
            // Given
            Option a = new Option("TAKE TORCH", 2);
            Option b = new Option(" take   \t torch  ", 3);
            // When
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(1, "text", List.of(a, b), false, null));
            // Then
            assertTrue(ex.getMessage().startsWith("duplicate option choice: "));
            assertEquals("duplicate option choice:  take   \t torch  ", ex.getMessage());
        }

        @Test
        void nonFinalNodeOptionWithNullNextThrowsTest() {
            // Given
            Option noNext = new Option("Open door", null);
            // When
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(1, "text", List.of(noNext), false, null));
            // Then
            assertEquals("non-final node option must have next: Open door", ex.getMessage());
        }
    }

    @Nested
    class TextLengthValidation {

        @Test
        void textOver10000ThrowsTest() {
            // Given
            String longText = "a".repeat(10_001);
            // When
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(1, longText, List.of(), false, null));
            // Then
            assertEquals("text too long", ex.getMessage());
        }
    }

    @Nested
    class GettersAndImageNormalization {

        @Test
        void gettersReturnValuesTest() {
            // Given
            List<Option> options = List.of(op1);
            // When
            QuestNode n = QuestNode.of(5, "hi", options, false, "/img/a.png");
            // Then
            assertEquals(5, n.getId());
            assertEquals("hi", n.getText());
            assertEquals(options, n.getOptions());
            assertFalse(n.isFin());
            assertEquals("/img/a.png", n.getImage());
        }

        @Test
        void imageNullStaysNullTest() {
            // Given
            // When
            QuestNode n = QuestNode.of(1, "t", List.of(), false, null);
            // Then
            assertNull(n.getImage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "\t", " \n "})
        void imageBlankBecomesNullTest(String blank) {
            // Given
            // When
            QuestNode n = QuestNode.of(1, "t", List.of(), false, blank);
            // Then
            assertNull(n.getImage());
        }

        @Test
        void imageIsTrimmedTest() {
            // Given
            String image = "  /path/pic.jpg  ";
            // When
            QuestNode n = QuestNode.of(1, "t", List.of(), false, image);
            // Then
            assertEquals("/path/pic.jpg", n.getImage());
        }
    }

    @Nested
    class ImmutabilityAndDefensiveCopies {

        @Test
        void optionsListIsUnmodifiableTest() {
            // Given
            QuestNode n = QuestNode.of(1, "t", List.of(op1, op2), false, null);
            List<Option> opts = n.getOptions();
            // When / Then
            assertThrows(UnsupportedOperationException.class, () -> {
                //noinspection Immutable
                opts.add(new Option("X", 9));
            });
        }

        @Test
        void originalListMutationDoesNotAffectNodeTest() {
            // Given
            List<Option> src = new ArrayList<>();
            src.add(op1);
            // When
            QuestNode n = QuestNode.of(1, "t", src, false, null);
            src.add(op2);
            // Then
            assertEquals(List.of(op1), n.getOptions());
        }
    }

    @Nested
    class OptionTextsMethod {

        @Test
        void optionTextsReturnChoicesInOrderTest() {
            // Given
            QuestNode n = QuestNode.of(1, "t", List.of(op1, op2), false, null);
            // When
            List<String> texts = n.optionTexts();
            // Then
            assertEquals(List.of("Go North", "Go South"), texts);
        }

        @Test
        void optionTextsListIsUnmodifiableTest() {
            // Given
            QuestNode n = QuestNode.of(1, "t", List.of(op1), false, null);
            List<String> texts = n.optionTexts();
            // When / Then
            assertThrows(UnsupportedOperationException.class, () -> {
                //noinspection Immutable
                texts.add("X");
            });
        }
    }

    @Nested
    class JsonJackson {

        private final ObjectMapper mapper = new ObjectMapper();

        @Test
        void deserializeFullJsonTest() throws Exception {
            // Given
            String json = """
                    {
                      "id": 12,
                      "text": "At the gate",
                      "options": [
                        {"choice":"Open the door", "next": 2},
                        {"choice":"Knock", "next": 3}
                      ],
                      "final": false,
                      "image": "/assets/1.jpg"
                    }
                    """;
            // When
            QuestNode n = mapper.readValue(json, QuestNode.class);
            // Then
            assertEquals(12, n.getId());
            assertEquals("At the gate", n.getText());
            assertEquals(2, n.getOptions().size());
            assertFalse(n.isFin());
            assertEquals("/assets/1.jpg", n.getImage());
        }

        @Test
        void deserializeWithoutOptionsGivesEmptyListTest() throws Exception {
            // Given
            String json = """
                    { "id": 1, "text": "Hello", "final": false }
                    """;
            // When
            QuestNode n = mapper.readValue(json, QuestNode.class);
            // Then
            assertNotNull(n.getOptions());
            assertTrue(n.getOptions().isEmpty());
        }

        @Test
        void deserializeFinalWithOptionsThrowsTest() {
            // Given
            String json = """
                    {
                      "id": 1,
                      "text": "End",
                      "final": true,
                      "options": [{"choice":"Anything", "next": 99}]
                    }
                    """;

            // When
            JsonProcessingException ex = assertThrows(JsonProcessingException.class,
                    () -> mapper.readValue(json, QuestNode.class));
            Throwable cause = ex.getCause();
            while (cause != null && cause.getCause() != null) {
                cause = cause.getCause();
            }
            // Then
            assertNotNull(cause);
            assertInstanceOf(IllegalArgumentException.class, cause);
            assertEquals("final node should not have options", cause.getMessage());
        }

        @Test
        void deserializeIgnoresUnknownPropertiesTest() throws Exception {
            // Given
            String json = """
                    { "id": 2, "text": "X", "final": false, "unknown": 123 }
                    """;
            // When
            QuestNode n = mapper.readValue(json, QuestNode.class);
            // Then
            assertEquals(2, n.getId());
            assertEquals("X", n.getText());
        }

        @Test
        void serializeOmitsNullImageTest() throws Exception {
            // Given
            QuestNode n = QuestNode.of(3, "T", List.of(), false, null);
            // When
            String json = mapper.writeValueAsString(n);
            // Then
            assertFalse(json.contains("\"image\""));
        }
    }
}