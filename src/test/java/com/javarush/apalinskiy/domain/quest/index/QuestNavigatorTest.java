package com.javarush.apalinskiy.domain.quest.index;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestNavigator")
class QuestNavigatorTest {

    private QuestNode nf(int id, Option... opts) {
        return QuestNode.nonFin(id, "n" + id, List.of(opts), null);
    }

    private QuestNode fin() {
        return QuestNode.fin(2, "end" + 2, null);
    }

    @Mock
    Option opt;
    @Mock
    QuestNode qn;

    @Nested
    @DisplayName("from() strict")
    class FromStrict {

        @Test
        @DisplayName("builds navigator when links valid then ok")
        void buildsOk() {
            // Given
            QuestNode a = nf(1, new Option("go", 2));
            QuestNode b = fin();
            // When
            QuestNavigator nav = QuestNavigator.from(List.of(a, b), 1);
            // Then
            assertEquals(1, nav.start().getId());
            assertSame(b, nav.get(2));
            assertEquals(Set.of(1, 2), nav.allIds());
        }

        @Test
        @DisplayName("throws when broken link then IllegalStateException")
        void throwsOnBrokenLink() {
            // Given
            QuestNode a = nf(1, new Option("go", 99));
            // When / Then
            IllegalStateException ex =
                    assertThrows(IllegalStateException.class, () -> QuestNavigator.from(List.of(a), 1));
            assertTrue(ex.getMessage().contains("Broken link: #1 -> #99"));
        }

        @Test
        @DisplayName("throws when startId not present then IllegalArgumentException")
        void throwsWhenStartMissing() {
            // Given
            QuestNode only = nf(2);
            // When / Then
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> QuestNavigator.from(List.of(only), 1));
            assertTrue(ex.getMessage().startsWith("Node not found: id="));
        }

        @Test
        @DisplayName("throws NPE when nodes=null then error")
        void throwsOnNullNodes() {
            // Given / When / Then
            assertThrows(NullPointerException.class, () -> QuestNavigator.from(null, 1));
        }

        @Test
        @DisplayName("ignores option with next=null in strict mode (via mocks) then no throw and choose empty")
        void strictSkipsNullNext_viaMocks() {
            // Given
            when(opt.next()).thenReturn(null);
            when(qn.getId()).thenReturn(1);
            when(qn.getOptions()).thenReturn(List.of(opt));
            QuestNode fin = QuestNode.fin(2, "end2", null);
            // When
            QuestNavigator nav = QuestNavigator.from(List.of(qn, fin), 1);
            // Then
            assertNotNull(nav.start());
            assertTrue(nav.choose(1, "ghost").isEmpty());
        }
    }

    @Nested
    @DisplayName("editingFrom() lenient")
    class EditingFromLenient {

        @Test
        @DisplayName("builds even with broken link then choose() empty")
        void buildsWithBrokenLink_chooseEmpty() {
            // Given
            QuestNode a = nf(1, new Option("go", 99));
            // When
            QuestNavigator nav = QuestNavigator.editingFrom(List.of(a), 1);
            // Then
            assertTrue(nav.choose(1, "go").isEmpty());
        }

        @Test
        @DisplayName("throws when startId not present then IllegalArgumentException")
        void throwsWhenStartMissing() {
            // Given
            QuestNode only = nf(2);
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> QuestNavigator.editingFrom(List.of(only), 1));
        }
    }

    @Nested
    @DisplayName("choose()")
    class ChooseMethod {

        @Test
        @DisplayName("returns next node when exact answer then present")
        void returnsOnExact() {
            // Given
            QuestNode a = nf(1, new Option("go", 2));
            QuestNode b = fin();
            QuestNavigator nav = QuestNavigator.from(List.of(a, b), 1);
            // When
            Optional<QuestNode> res = nav.choose(1, "go");
            // Then
            assertTrue(res.isPresent());
            assertEquals(2, res.get().getId());
        }

        @Test
        @DisplayName("normalizes answer when mixed whitespace/case then resolves")
        void normalizesAnswer() {
            // Given
            QuestNode a = nf(1, new Option("Open   Door", 2));
            QuestNode b = fin();
            QuestNavigator nav = QuestNavigator.from(List.of(a, b), 1);
            // When
            Optional<QuestNode> res = nav.choose(1, "  open \t door  ");
            // Then
            assertTrue(res.isPresent());
            assertEquals(2, res.get().getId());
        }

        @Test
        @DisplayName("returns empty when no mapping then empty")
        void returnsEmptyWhenNoMapping() {
            // Given
            QuestNode a = nf(1, new Option("take key", 2));
            QuestNode b = fin();
            QuestNavigator nav = QuestNavigator.from(List.of(a, b), 1);
            // When
            Optional<QuestNode> res = nav.choose(1, "drop key");
            // Then
            assertTrue(res.isEmpty());
        }
    }

    @Nested
    @DisplayName("start(), get(), allIds()")
    class Accessors {

        @Test
        @DisplayName("start() returns start node then correct id")
        void startReturnsStart() {
            // Given
            QuestNode a = nf(10);
            QuestNavigator nav = QuestNavigator.editingFrom(List.of(a), 10);
            // When
            QuestNode s = nav.start();
            // Then
            assertEquals(10, s.getId());
        }

        @Test
        @DisplayName("get() returns null when absent then null")
        void getReturnsNullWhenAbsent() {
            // Given
            QuestNavigator nav = QuestNavigator.editingFrom(List.of(nf(1)), 1);
            // When
            QuestNode n = nav.get(99);
            // Then
            assertNull(n);
        }

        @Test
        @DisplayName("allIds() returns set of ids then equals")
        void allIdsReturnsSet() {
            // Given
            QuestNavigator nav = QuestNavigator.editingFrom(List.of(nf(1), nf(2), nf(3)), 1);
            // When
            Set<Integer> ids = nav.allIds();
            // Then
            assertEquals(Set.of(1, 2, 3), ids);
        }
    }
}