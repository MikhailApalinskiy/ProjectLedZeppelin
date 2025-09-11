package com.javarush.apalinskiy.domain.quest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QuestNode")
class QuestNodeTest {

    private Option opt(String choice, Integer next) {
        return new Option(choice, next);
    }

    @Nested
    @DisplayName("factories")
    class Factories {

        @Test
        @DisplayName("of(): builds non-final node when valid then ok")
        void ofBuildsNonFinal() {
            // Given
            List<Option> options = List.of(opt("go", 2));
            // When
            QuestNode n = QuestNode.of(1, "text", options, false, "img.png");
            // Then
            assertEquals(1, n.getId());
        }

        @Test
        @DisplayName("fin(): builds final node with empty options then ok")
        void finBuildsFinal() {
            // Given / When
            QuestNode n = QuestNode.fin(2, "end", null);
            // Then
            assertTrue(n.isFin());
        }

        @Test
        @DisplayName("nonFin(): builds node with options then ok")
        void nonFinBuilds() {
            // Given
            QuestNode n = QuestNode.nonFin(3, "text", List.of(opt("go", 4)), null);
            // When / Then
            assertFalse(n.isFin());
        }

        @Test
        @DisplayName("json(): maps fields including 'final' then ok")
        void jsonBuilds() {
            // Given
            List<Option> opts = List.of(opt("go", 2));
            // When
            QuestNode n = QuestNode.json(10, "t", opts, false, " img ");
            // Then
            assertEquals(10, n.getId());
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("throws when id <= 0 then IAE")
        void idMustBePositive() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(0, "t", List.of(), false, null));
        }

        @Test
        @DisplayName("throws when text is blank then IAE")
        void textNotBlank() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(1, "   ", List.of(), false, null));
        }

        @Test
        @DisplayName("throws when final node has options then IAE")
        void finalNodeNoOptions() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(1, "t", List.of(opt("x", 2)), true, null));
        }

        @Test
        @DisplayName("throws when non-final option has next=null then IAE")
        void nonFinalOptionNeedsNext() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.nonFin(1, "t", List.of(opt("x", null)), null));
        }

        @Test
        @DisplayName("throws when duplicate normalized options then IAE")
        void duplicateNormalizedChoice() {
            // Given
            List<Option> opts = List.of(opt(" Go  North ", 2), opt("go north", 3));
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.nonFin(5, "t", opts, null));
        }

        @Test
        @DisplayName("throws when text too long (>10000) then IAE")
        void textTooLong() {
            // Given
            String longText = "x".repeat(10_001);
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> QuestNode.of(1, longText, List.of(), false, null));
        }
    }

    @Nested
    @DisplayName("options list")
    class OptionsList {

        @Test
        @DisplayName("is defensive-copied when source mutated then node not affected")
        void defensiveCopy() {
            // Given
            List<Option> source = new ArrayList<>(List.of(opt("go", 2)));
            QuestNode n = QuestNode.nonFin(1, "t", source, null);
            // When
            source.add(opt("stay", 3));
            // Then
            assertEquals(1, n.getOptions().size());
        }

        @Test
        @DisplayName("is unmodifiable when accessed then UOE")
        void unmodifiableOnGetter() {
            // Given
            QuestNode n = QuestNode.nonFin(1, "t", List.of(opt("go", 2)), null);
            // When / Then
            //noinspection DataFlowIssue
            assertThrows(UnsupportedOperationException.class,
                    () -> n.getOptions().add(opt("x", 3)));
        }

        @Test
        @DisplayName("defaults to empty when options=null then empty list")
        void defaultsToEmptyWhenNull() {
            // Given / When
            QuestNode n = QuestNode.of(1, "t", null, false, null);
            // Then
            assertTrue(n.getOptions().isEmpty());
        }
    }

    @Nested
    @DisplayName("image normalization")
    class ImageNormalization {

        @Test
        @DisplayName("trims non-blank image when provided then no surrounding spaces")
        void trimsImage() {
            // Given / When
            QuestNode n = QuestNode.of(1, "t", List.of(), false, " pic.png ");
            // Then
            assertEquals("pic.png", n.getImage());
        }

        @Test
        @DisplayName("null when image blank then null")
        void blankBecomesNull() {
            // Given / When
            QuestNode n = QuestNode.of(1, "t", List.of(), false, "   ");
            // Then
            assertNull(n.getImage());
        }
    }
}