package com.javarush.apalinskiy.quest.core;

import com.javarush.apalinskiy.quest.model.Option;
import com.javarush.apalinskiy.quest.model.QuestNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestChoiceIndexTest {
    private QuestNode n1;
    private QuestNode n2;

    @BeforeEach
    void setUp() {
        n1 = QuestNode.of(1, "Node 1", List.of(
                new Option("Open door", 2),
                new Option("Knock", 3)
        ), false, null);
        n2 = QuestNode.of(2, "Node 2", List.of(
                new Option("Continue", 4)
        ), false, null);
    }

    @Nested
    class FromFactory {

        @Test
        void nullNodesThrowsNpeTest() {
            // Given
            // When
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> QuestChoiceIndex.from(null));
            // Then
            assertEquals("nodes", ex.getMessage());
        }

        @Test
        void emptyNodesProducesEmptyIndexTest() {
            // Given
            List<QuestNode> nodes = List.of();
            // When
            QuestChoiceIndex index = QuestChoiceIndex.from(nodes);
            // Then
            assertNull(index.nextId(1, "anything"));
            assertNull(index.nextId(999, "Open door"));
            assertNull(index.nextId(0, null));
        }

        @Test
        void buildsIndexForSingleNodeAndOptionTest() {
            // Given
            QuestNode single = QuestNode.of(10, "S", List.of(
                    new Option("  GO  \t  NORTH ", 42)
            ), false, null);
            // When
            QuestChoiceIndex index = QuestChoiceIndex.from(List.of(single));
            // Then
            assertEquals(42, index.nextId(10, "go north"));
        }

        @Test
        void duplicateNormalizedChoicesInSameNodeIdAcrossListThrowIseTest() {
            // Given
            QuestNode node1 = QuestNode.of(7, "dup-1",
                    List.of(new Option("TAKE TORCH", 2)), false, null);
            QuestNode node2 = QuestNode.of(7, "dup-2",
                    List.of(new Option("  take \t  torch  ", 3)), false, null);
            // When / Then
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> QuestChoiceIndex.from(List.of(node1, node2)));
            assertEquals("Duplicate choice in node #7 for answer: '  take \t  torch  '",
                    ex.getMessage());
        }

        @Test
        void sameChoicesInDifferentNodesDoNotConflictTest() {
            // Given
            QuestNode a = QuestNode.of(5, "A", List.of(new Option("Open", 11)), false, null);
            QuestNode b = QuestNode.of(6, "B", List.of(new Option("Open", 22)), false, null);
            // When
            QuestChoiceIndex index = QuestChoiceIndex.from(List.of(a, b));
            // Then
            assertEquals(11, index.nextId(5, "open"));
            assertEquals(22, index.nextId(6, "open"));
        }

        @Test
        void modifyingSourceListAfterBuildDoesNotAffectIndexTest() {
            // Given
            List<QuestNode> nodes = new ArrayList<>();
            nodes.add(n1);
            // When
            QuestChoiceIndex index = QuestChoiceIndex.from(nodes);
            nodes.add(n2);
            // Then
            assertEquals(2, index.nextId(1, "open door"));
            assertNull(index.nextId(2, "continue")); // узла 2 в индексе нет
        }

        @Test
        void skipsOptionsWithNullNextAndBuildsIndexFromOthersTest() {
            // Given
            QuestNode node = mock(QuestNode.class);
            when(node.getId()).thenReturn(100);
            when(node.getOptions()).thenReturn(List.of(
                    new Option("   \t  ", null),
                    new Option("Open", 42)
            ));
            // When
            QuestChoiceIndex index = QuestChoiceIndex.from(List.of(node));
            // Then
            assertNull(index.nextId(100, null));
            assertEquals(42, index.nextId(100, "open"));
        }
    }

    @Nested
    class NextIdLookup {

        @ParameterizedTest
        @CsvSource({
                "'Open door', 2",
                "'open door', 2",
                "'  OPEN   \t door  ', 2",
                "'open   door', 2"
        })
        void normalizesUserAnswerForLookupTest(String userAnswer, int expectedNext) {
            // Given
            QuestChoiceIndex index = QuestChoiceIndex.from(List.of(n1));
            // When
            Integer next = index.nextId(1, userAnswer);
            // Then
            assertEquals(expectedNext, next);
        }

        @Test
        void nullUserAnswerSearchesByEmptyStringTest() {
            // Given
            QuestNode node = QuestNode.of(9, "Blank", List.of(
                    new Option("   \t  ", 77)
            ), false, null);
            QuestChoiceIndex index = QuestChoiceIndex.from(List.of(node));
            // When
            Integer next = index.nextId(9, null);
            // Then
            assertEquals(77, next);
        }

        @Test
        void returnsNullWhenNoMatchingChoiceTest() {
            // Given
            QuestChoiceIndex index = QuestChoiceIndex.from(List.of(n1));
            // When
            Integer next = index.nextId(1, "does not exist");
            // Then
            assertNull(next);
        }
    }

}