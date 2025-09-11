package com.javarush.apalinskiy.domain.quest.index;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QuestIdIndex")
class QuestIdIndexTest {

    private QuestNode nf(int id) {
        return QuestNode.nonFin(id, "n" + id, List.of(), null);
    }

    @Nested
    @DisplayName("from()")
    class FromFactory {

        @Test
        @DisplayName("throws NPE when nodes=null then error")
        void throwsOnNullList() {
            // Given / When / Then
            assertThrows(NullPointerException.class, () -> QuestIdIndex.from(null));
        }

        @Test
        @DisplayName("throws NPE when list contains null element then error")
        void throwsOnNullElement() {
            // Given
            List<QuestNode> list = new ArrayList<>();
            list.add(nf(1));
            list.add(null);
            // When / Then
            assertThrows(NullPointerException.class, () -> QuestIdIndex.from(list));
        }

        @Test
        @DisplayName("throws ISE when duplicate id in list then error")
        void throwsOnDuplicateId() {
            // Given
            QuestNode a = nf(5);
            QuestNode b = nf(5);
            // When / Then
            IllegalStateException ex =
                    assertThrows(IllegalStateException.class, () -> QuestIdIndex.from(List.of(a, b)));
            assertTrue(ex.getMessage().contains("Duplicate node id: 5"));
        }

        @Test
        @DisplayName("builds index when unique ids then ok")
        void buildsIndex() {
            // Given
            List<QuestNode> list = List.of(nf(1), nf(2));
            // When
            QuestIdIndex idx = QuestIdIndex.from(list);
            // Then
            assertEquals(2, idx.size());
        }
    }

    @Nested
    @DisplayName("get()")
    class GetMethod {

        @Test
        @DisplayName("returns node when present then same instance")
        void returnsWhenPresent() {
            // Given
            QuestNode a = nf(7);
            QuestIdIndex idx = QuestIdIndex.from(List.of(a, nf(8)));
            // When
            QuestNode got = idx.get(7);
            // Then
            assertSame(a, got);
        }

        @Test
        @DisplayName("returns null when absent then null")
        void returnsNullWhenAbsent() {
            // Given
            QuestIdIndex idx = QuestIdIndex.from(List.of(nf(1)));
            // When
            QuestNode got = idx.get(99);
            // Then
            assertNull(got);
        }
    }

    @Nested
    @DisplayName("require()")
    class RequireMethod {

        @Test
        @DisplayName("returns node when present then ok")
        void returnsWhenPresent() {
            // Given
            QuestIdIndex idx = QuestIdIndex.from(List.of(nf(3)));
            // When
            QuestNode got = idx.require(3);
            // Then
            assertEquals(3, got.getId());
        }

        @Test
        @DisplayName("throws IAE when absent then error")
        void throwsWhenAbsent() {
            // Given
            QuestIdIndex idx = QuestIdIndex.from(List.of(nf(1)));
            // When / Then
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> idx.require(42));
            assertTrue(ex.getMessage().startsWith("Node not found: id="));
        }
    }

    @Nested
    @DisplayName("size()")
    class SizeMethod {

        @Test
        @DisplayName("returns number of unique ids then equals")
        void returnsCount() {
            // Given
            QuestIdIndex idx = QuestIdIndex.from(List.of(nf(1), nf(2), nf(3)));
            // When
            int size = idx.size();
            // Then
            assertEquals(3, size);
        }
    }

    @Nested
    @DisplayName("allIds()")
    class AllIdsMethod {

        @Test
        @DisplayName("returns unmodifiable set when modified then UOE")
        void unmodifiable() {
            // Given
            QuestIdIndex idx = QuestIdIndex.from(List.of(nf(1)));
            // When
            Set<Integer> ids = idx.allIds();
            // Then
            //noinspection DataFlowIssue
            assertThrows(UnsupportedOperationException.class, () -> ids.add(2));
        }

        @Test
        @DisplayName("contains all ids when built then equals set")
        void containsAllIds() {
            // Given
            QuestIdIndex idx = QuestIdIndex.from(List.of(nf(10), nf(20), nf(30)));
            // When
            Set<Integer> ids = idx.allIds();
            // Then
            assertEquals(Set.of(10, 20, 30), ids);
        }
    }
}