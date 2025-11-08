package com.javarush.apalinskiy.domain.quest;

import com.javarush.apalinskiy.domain.quest.choice.ChoiceNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@DisplayName("Option entity")
class OptionTest {

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should initialize with valid choice and next")
        void initializesWithValidValues() {
            // Given
            String choice = "Go left";
            Integer next = 5;
            // When
            Option option = new Option(choice, next);
            // Then
            assertEquals(choice, option.getChoice());
            assertEquals(next, option.getNext());
            assertNull(option.getChoiceId());
            assertNull(option.getNode());
        }

        @Test
        @DisplayName("should allow null next")
        void allowsNullNext() {
            // Given
            String choice = "Go right";
            // When
            Option option = new Option(choice, null);
            // Then
            assertEquals(choice, option.getChoice());
            assertNull(option.getNext());
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("should throw when choice is null")
        void throwsWhenChoiceIsNull() {
            // Given
            // When / Then
            assertThrows(NullPointerException.class, () -> new Option(null, 1));
        }

        @Test
        @DisplayName("should throw when choice is too long")
        void throwsWhenChoiceTooLong() {
            // Given
            String longText = "a".repeat(256);
            // When / Then
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Option(longText, 1)
            );
            assertTrue(ex.getMessage().contains("too long"));
        }
    }

    @Nested
    @DisplayName("normalizedChoice()")
    class NormalizedChoiceTests {

        @Test
        @DisplayName("should normalize text via ChoiceNormalizer")
        void usesChoiceNormalizer() {
            // Given
            String choice = "  Hello   WORLD  ";
            Option option = new Option(choice, 1);
            // When
            try (MockedStatic<ChoiceNormalizer> normalizer = mockStatic(ChoiceNormalizer.class)) {
                normalizer.when(() -> ChoiceNormalizer.normalize(choice))
                        .thenReturn("hello world");
                // Then
                String result = option.normalizedChoice();
                assertEquals("hello world", result);
                normalizer.verify(() -> ChoiceNormalizer.normalize(choice));
            }
        }
    }

    @Nested
    @DisplayName("setters and getters")
    class Accessors {

        @Test
        @DisplayName("should correctly set and return all fields")
        void setsAndGetsAllFields() {
            // Given
            Option option = new Option("Take path", 10);
            QuestNode node = new QuestNode();
            // When
            option.setChoiceId(42L);
            option.setNode(node);
            // Then
            assertAll(
                    () -> assertEquals(42L, option.getChoiceId()),
                    () -> assertEquals("Take path", option.getChoice()),
                    () -> assertEquals(10, option.getNext()),
                    () -> assertSame(node, option.getNode())
            );
        }
    }
}