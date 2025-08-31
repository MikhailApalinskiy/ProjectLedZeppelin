package com.javarush.apalinskiy.quest.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.javarush.apalinskiy.quest.model.QuestNode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestJsonReaderTest {
    private final QuestJsonReader reader = new QuestJsonReader();

    @Nested
    class ErrorCases {

        @Test
        void readNullReaderThrowsIaeFromJacksonTest() {
            // Given
            // When
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> reader.read(null));
            // Then
            assertTrue(ex.getMessage().contains("argument \"r\" is null"));
        }

        @Test
        void readMalformedJsonThrowsJsonProcessingExceptionTest() {
            // Given
            String json = "[ { \"id\":1, \"text\":\"x\", \"final\":false } ";
            Reader r = new StringReader(json);
            // When / Then
            assertThrows(JsonProcessingException.class, () -> reader.read(r));
        }

        @Test
        void readObjectInsteadOfArrayThrowsJsonProcessingExceptionTest() {
            // Given
            String json = "{ \"id\":1, \"text\":\"x\", \"final\":false }";
            Reader r = new StringReader(json);
            // When / Then
            assertThrows(JsonProcessingException.class, () -> reader.read(r));
        }

        @Test
        void readFinalNodeWithOptionsFailsValidationTest() {
            // Given
            String json = """
                    [
                      {
                        "id": 1,
                        "text": "End",
                        "final": true,
                        "options": [ {"choice":"Anything","next": 2} ]
                      }
                    ]
                    """;
            Reader r = new StringReader(json);
            // When / Then
            assertThrows(JsonProcessingException.class, () -> reader.read(r));
        }
    }

    @Nested
    class SuccessCases {

        @Test
        void readEmptyArrayReturnsEmptyListTest() throws Exception {
            // Given
            Reader r = new StringReader("[]");
            // When
            List<QuestNode> nodes = reader.read(r);
            // Then
            assertNotNull(nodes);
            assertTrue(nodes.isEmpty());
        }

        @Test
        void readSingleNodeOkTest() throws Exception {
            // Given
            String json = """
                    [
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
                    ]
                    """;
            Reader r = new StringReader(json);
            // When
            List<QuestNode> nodes = reader.read(r);
            // Then
            assertEquals(1, nodes.size());
            QuestNode n = nodes.getFirst();
            assertEquals(12, n.getId());
            assertEquals("At the gate", n.getText());
            assertFalse(n.isFin());
            assertEquals("/assets/1.jpg", n.getImage());
            assertEquals(2, n.getOptions().size());
            assertEquals("Open the door", n.getOptions().getFirst().choice());
            assertEquals(2, n.getOptions().getFirst().next());
        }

        @Test
        void readNodeWithoutOptionsProducesEmptyOptionsListTest() throws Exception {
            // Given
            String json = """
                    [
                      { "id": 1, "text": "Hello", "final": false }
                    ]
                    """;
            Reader r = new StringReader(json);
            // When
            List<QuestNode> nodes = reader.read(r);
            // Then
            assertEquals(1, nodes.size());
            assertTrue(nodes.getFirst().getOptions().isEmpty());
        }

        @Test
        void readIgnoresUnknownFieldsInsideNodesAndOptionsTest() throws Exception {
            // Given
            String json = """
                    [
                      {
                        "id": 2,
                        "text": "X",
                        "final": false,
                        "unknown_field": 123,
                        "options": [
                          {"choice":"Go", "next": 5, "extra": "zzz"}
                        ]
                      }
                    ]
                    """;
            Reader r = new StringReader(json);
            // When
            List<QuestNode> nodes = reader.read(r);
            // Then
            assertEquals(1, nodes.size());
            QuestNode n = nodes.getFirst();
            assertEquals(2, n.getId());
            assertEquals("X", n.getText());
            assertFalse(n.isFin());
            assertEquals(1, n.getOptions().size());
            assertEquals("Go", n.getOptions().getFirst().choice());
            assertEquals(5, n.getOptions().getFirst().next());
        }

        @Test
        void readUnicodeTextAndBlankImageHandledByQuestNodeTest() throws Exception {
            // Given
            String json = """
                    [
                      {
                        "id": 3,
                        "text": "Привет, мир — ÄÖÜ",
                        "final": false,
                        "image": "   \\t   "
                      }
                    ]
                    """;
            Reader r = new StringReader(json);
            // When
            List<QuestNode> nodes = reader.read(r);
            // Then
            assertEquals(1, nodes.size());
            QuestNode n = nodes.getFirst();
            assertEquals("Привет, мир — ÄÖÜ", n.getText());
            assertNull(n.getImage());
        }
    }
}