package com.javarush.apalinskiy.domain.quest.index;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("QuestChoiceIndex")
class QuestChoiceIndexTest {

    private QuestNode nf(int id, Option... opts) {
        return QuestNode.nonFin(id, "n" + id, List.of(opts), null);
    }

    @Nested
    @DisplayName("from()")
    class FromFactory {

        @Test
        @DisplayName("skips option when next=null (via mocks) then no mapping")
        void skipsOptionWhenNextNull_viaMocks() {
            // Given
            Option opt = mock(Option.class);
            when(opt.next()).thenReturn(null);
            when(opt.normalizedChoice()).thenReturn("ghost");
            when(opt.choice()).thenReturn("ghost");
            QuestNode qn = mock(QuestNode.class);
            when(qn.getId()).thenReturn(42);
            when(qn.getOptions()).thenReturn(List.of(opt));
            // When
            QuestChoiceIndex idx = QuestChoiceIndex.from(List.of(qn));
            // Then
            assertNull(idx.nextId(42, "ghost"));
        }

        @Test
        @DisplayName("throws NPE when nodes=null then error")
        void throwsOnNull() {
            // Given / When / Then
            assertThrows(NullPointerException.class, () -> QuestChoiceIndex.from(null));
        }

        @Test
        @DisplayName("builds index for single node when one option then nextId resolves")
        void buildsSingleMapping() {
            // Given
            QuestNode n1 = nf(1, new Option("go north", 2));
            // When
            QuestChoiceIndex idx = QuestChoiceIndex.from(List.of(n1));
            // Then
            assertEquals(2, idx.nextId(1, "go north"));
        }

        @Test
        @DisplayName("allows same choice text in different node ids when built then no conflict")
        void sameChoiceDifferentNodesOk() {
            // Given
            QuestNode a = nf(10, new Option("open", 11));
            QuestNode b = nf(20, new Option("open", 21));
            // When
            QuestChoiceIndex idx = QuestChoiceIndex.from(List.of(a, b));
            // Then
            assertEquals(11, idx.nextId(10, "open"));
            assertEquals(21, idx.nextId(20, "open"));
        }

        @Test
        @DisplayName("throws when duplicate normalized choice for same fromId across nodes then ISE")
        void throwsOnDuplicateNormalizedChoiceSameFromAcrossNodes() {
            // Given
            QuestNode n1 = nf(5, new Option(" Go  North ", 9));
            QuestNode n2 = nf(5, new Option("go north", 7));
            // When / Then
            IllegalStateException ex =
                    assertThrows(IllegalStateException.class, () -> QuestChoiceIndex.from(List.of(n1, n2)));
            assertTrue(ex.getMessage().contains("Duplicate choice in node #5"));
            assertTrue(ex.getMessage().contains("for answer"));
            assertTrue(ex.getMessage().contains("go north"));
        }
    }

    @Nested
    @DisplayName("nextId()")
    class NextIdLookup {

        @Test
        @DisplayName("returns mapped id when exact match then ok")
        void returnsOnExact() {
            // Given
            QuestNode n1 = nf(1, new Option("take key", 3));
            QuestChoiceIndex idx = QuestChoiceIndex.from(List.of(n1));
            // When
            Integer next = idx.nextId(1, "take key");
            // Then
            assertEquals(3, next);
        }

        @Test
        @DisplayName("normalizes user answer when mixed whitespace/case then resolves")
        void normalizesUserAnswer() {
            // Given
            QuestNode n1 = nf(2, new Option("Open   Door", 4));
            QuestChoiceIndex idx = QuestChoiceIndex.from(List.of(n1));
            // When
            Integer next = idx.nextId(2, "  open \t door  ");
            // Then
            assertEquals(4, next);
        }

        @Test
        @DisplayName("returns null when no mapping for answer then null")
        void returnsNullWhenNoMapping() {
            // Given
            QuestNode n1 = nf(1, new Option("take key", 3));
            QuestChoiceIndex idx = QuestChoiceIndex.from(List.of(n1));
            // When
            Integer next = idx.nextId(1, "drop key");
            // Then
            assertNull(next);
        }

        @Test
        @DisplayName("keys are scoped by fromId when same text different nodes then separate")
        void scopedByFromId() {
            // Given
            QuestNode a = nf(7, new Option("use", 8));
            QuestNode b = nf(9, new Option("use", 10));
            QuestChoiceIndex idx = QuestChoiceIndex.from(List.of(a, b));
            // When
            Integer n1 = idx.nextId(7, "use");
            Integer n2 = idx.nextId(9, "use");
            // Then
            assertEquals(8, n1);
            assertEquals(10, n2);
        }

        @Test
        @DisplayName("treats tabs/newlines as spaces when normalized then resolves")
        void handlesTabsAndNewlines() {
            // Given
            QuestNode n1 = nf(3, new Option("say hello world", 4));
            QuestChoiceIndex idx = QuestChoiceIndex.from(List.of(n1));
            // When
            Integer next = idx.nextId(3, "say\thello\nworld");
            // Then
            assertEquals(4, next);
        }
    }
}