package com.javarush.apalinskiy.domain.quest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestNode entity")
class QuestNodeTest {

    @Mock
    Option optionA;

    @Mock
    Option optionB;

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("of() should create valid node with provided values")
        void createsNodeWithOf() {
            // Given
            when(optionA.normalizedChoice()).thenReturn("go");
            when(optionA.getNext()).thenReturn(2);
            List<Option> options = List.of(optionA);
            // When
            QuestNode node = QuestNode.of(1, "Text", options, false, "img.png");
            // Then
            assertEquals(1, node.getId());
            assertEquals("Text", node.getText());
            assertEquals("img.png", node.getImage());
            assertFalse(node.getFin());
            assertEquals(1, node.getOptions().size());
            assertTrue(node.getOptions().contains(optionA));
            verify(optionA).setNode(node);
        }

        @Test
        @DisplayName("fin() should create final node with empty options")
        void createsFinalNode() {
            // Given / When
            QuestNode node = QuestNode.fin(1, "The End", "end.png");
            // Then
            assertTrue(node.getFin());
            assertEquals("The End", node.getText());
            assertTrue(node.getOptions().isEmpty());
            assertEquals("end.png", node.getImage());
        }

        @Test
        @DisplayName("nonFin() should create non-final node with options")
        void createsNonFinalNode() {
            // Given
            when(optionA.normalizedChoice()).thenReturn("next");
            when(optionA.getNext()).thenReturn(2);
            // When
            QuestNode node = QuestNode.nonFin(2, "Continue", List.of(optionA), null);
            // Then
            assertFalse(node.getFin());
            assertEquals("Continue", node.getText());
            assertEquals(1, node.getOptions().size());
            assertTrue(node.getOptions().contains(optionA));
            verify(optionA).setNode(node);
        }

        @Test
        @DisplayName("json() should delegate to private constructor")
        void createsNodeFromJson() {
            // Given
            when(optionA.normalizedChoice()).thenReturn("go");
            when(optionA.getNext()).thenReturn(2);
            // When
            QuestNode node = QuestNode.json(1, "Story", List.of(optionA), false, "pic.jpg");
            // Then
            assertEquals(1, node.getId());
            assertEquals("Story", node.getText());
            assertEquals("pic.jpg", node.getImage());
            assertFalse(node.getFin());
            assertTrue(node.getOptions().contains(optionA));
            verify(optionA).setNode(node);
        }
    }

    @Nested
    @DisplayName("validation rules")
    class ValidationRules {

        @Test
        @DisplayName("should throw when id ≤ 0")
        void throwsWhenInvalidId() {
            // Given
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(0, "abc", List.of(), true, null));
        }

        @Test
        @DisplayName("should throw when text blank")
        void throwsWhenTextBlank() {
            // Given
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(1, "   ", List.of(), false, null));
        }

        @Test
        @DisplayName("should throw when final node has options")
        void throwsWhenFinalNodeHasOptions() {
            // Given
            List<Option> options = List.of(optionA);
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(1, "End", options, true, null));
        }

        @Test
        @DisplayName("should throw when text too long")
        void throwsWhenTextTooLong() {
            // Given
            String longText = "a".repeat(10_001);
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(1, longText, List.of(), true, null));
        }

        @Test
        @DisplayName("should throw when duplicate normalized options")
        void throwsWhenDuplicateChoices() {
            // Given
            when(optionA.normalizedChoice()).thenReturn("dup");
            when(optionB.normalizedChoice()).thenReturn("dup");
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(1, "Node", List.of(optionA, optionB), false, null));
        }

        @Test
        @DisplayName("should throw when non-final node option has null next")
        void throwsWhenOptionNextNull() {
            // Given
            when(optionA.normalizedChoice()).thenReturn("x");
            when(optionA.getNext()).thenReturn(null);
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(1, "Node", List.of(optionA), false, null));
        }
    }

    @Nested
    @DisplayName("setOptionsSafe()")
    class SetOptionsSafe {

        @Test
        @DisplayName("should assign options and set node reference")
        void assignsOptionsAndLinksBack() {
            // Given
            QuestNode node = QuestNode.fin(1, "End", null);
            // When
            node.setOptionsSafe(List.of(optionA));
            // Then
            verify(optionA).setNode(node);
            assertEquals(1, node.getOptions().size());
            assertTrue(node.getOptions().contains(optionA));
        }

        @Test
        @DisplayName("should clear options when null provided")
        void clearsWhenNull() {
            // Given
            QuestNode node = QuestNode.fin(1, "End", null);
            node.setOptionsSafe(List.of(optionA));
            // When
            node.setOptionsSafe(null);
            // Then
            assertTrue(node.getOptions().isEmpty());
        }

        @Test
        @DisplayName("should skip null options")
        void skipsNulls() {
            // Given
            QuestNode node = QuestNode.fin(1, "End", null);
            // When
            node.setOptionsSafe(Arrays.asList(null, null));
            // Then
            assertTrue(node.getOptions().isEmpty());
        }
    }

    @Nested
    @DisplayName("image trimming and normalization")
    class ImageNormalization {

        @Test
        @DisplayName("should trim image value")
        void trimsImage() {
            // Given / When
            QuestNode node = QuestNode.of(1, "Node", List.of(), false, "   img.png  ");
            // Then
            assertEquals("img.png", node.getImage());
        }

        @Test
        @DisplayName("should set image null when blank")
        void nullsBlankImage() {
            // Given / When
            QuestNode node = QuestNode.of(1, "Node", List.of(), false, "  ");
            // Then
            assertNull(node.getImage());
        }
    }
}