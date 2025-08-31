package com.javarush.apalinskiy.quest.core;

import com.javarush.apalinskiy.quest.model.Option;
import com.javarush.apalinskiy.quest.model.QuestNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QuestIdIndexTest {
    private QuestNode n1;
    private QuestNode n2;

    @BeforeEach
    void setUp() {
        n1 = QuestNode.of(1, "Start", List.of(new Option("Go", 2)), false, null);
        n2 = QuestNode.of(2, "Next", List.of(), true, null);
    }

    @Nested
    class FromFactory {

        @Test
        void nullNodesThrowsNpeTest() {
            // Given
            // When / Then
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> QuestIdIndex.from(null));
            assertEquals("nodes", ex.getMessage());
        }

        @Test
        void nodesContainingNullElementThrowsNpeTest() {
            // Given
            List<QuestNode> nodes = new ArrayList<>();
            nodes.add(n1);
            nodes.add(null);
            // When / Then
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> QuestIdIndex.from(nodes));
            assertEquals("node", ex.getMessage());
        }

        @Test
        void duplicateIdThrowsIseTest() {
            // Given
            QuestNode dup1 = QuestNode.of(5, "A", List.of(), true, null);
            QuestNode dup2 = QuestNode.of(5, "B", List.of(), true, null);
            // When / Then
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> QuestIdIndex.from(List.of(dup1, dup2)));
            assertEquals("Duplicate node id: 5", ex.getMessage());
        }

        @Test
        void emptyNodesCreatesEmptyIndexTest() {
            // Given
            List<QuestNode> nodes = List.of();
            // When
            QuestIdIndex index = QuestIdIndex.from(nodes);
            // Then
            assertEquals(0, index.size());
            assertTrue(index.allIds().isEmpty());
        }

        @Test
        void validNodesBuildsIndexTest() {
            // Given
            List<QuestNode> nodes = List.of(n1, n2);
            // When
            QuestIdIndex index = QuestIdIndex.from(nodes);
            // Then
            assertEquals(2, index.size());
            assertEquals(Set.of(1, 2), index.allIds());
        }
    }

    @Nested
    class GetAndRequire {

        @Test
        void getReturnsNodeIfPresentTest() {
            // Given
            QuestIdIndex index = QuestIdIndex.from(List.of(n1, n2));
            // When
            QuestNode result = index.get(1);
            // Then
            assertEquals(n1, result);
        }

        @Test
        void getReturnsNullIfNotPresentTest() {
            // Given
            QuestIdIndex index = QuestIdIndex.from(List.of(n1));
            // When
            QuestNode result = index.get(999);
            // Then
            assertNull(result);
        }

        @Test
        void requireReturnsNodeIfPresentTest() {
            // Given
            QuestIdIndex index = QuestIdIndex.from(List.of(n1, n2));
            // When
            QuestNode result = index.require(2);
            // Then
            assertEquals(n2, result);
        }

        @Test
        void requireThrowsIfNotPresentTest() {
            // Given
            QuestIdIndex index = QuestIdIndex.from(List.of(n1));
            // When / Then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> index.require(123));
            assertEquals("Node not found: id=123", ex.getMessage());
        }
    }

    @Nested
    class AllIdsAndDefensiveCopy {

        @Test
        void allIdsUnmodifiableTest() {
            // Given
            QuestIdIndex index = QuestIdIndex.from(List.of(n1));
            // When
            Set<Integer> ids = index.allIds();
            // Then
            assertThrows(UnsupportedOperationException.class, () -> ids.add(99));
        }

        @Test
        void allIdsNotAffectedByExternalListTest() {
            // Given
            List<QuestNode> src = new ArrayList<>();
            src.add(n1);
            QuestIdIndex index = QuestIdIndex.from(src);
            // When
            src.add(n2);
            // Then
            assertEquals(1, index.size());
            assertEquals(Set.of(1), index.allIds());
        }
    }
}