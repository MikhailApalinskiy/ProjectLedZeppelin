package com.javarush.apalinskiy.domain.quest.custom;

import static org.junit.jupiter.api.Assertions.*;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@DisplayName("CustomQuest")
class CustomQuestTest {

    private QuestNode n(int id) {
        return QuestNode.of(id, "text-" + id, List.of(), false, null);
    }

    @Nested
    @DisplayName("constructor")
    class Ctor {

        @Test
        @DisplayName("sets fields when valid args then ok")
        void setsFields() {
            // Given
            List<QuestNode> nodes = List.of(n(1), n(2));
            Instant now = Instant.now();
            // When
            CustomQuest q = new CustomQuest("id1", "owner", "name", 1, nodes, true, "v1", now, now);
            // Then
            assertEquals("id1", q.getId());
            assertEquals("owner", q.getOwnerId());
            assertEquals("name", q.getName());
            assertEquals(1, q.getStartId());
            assertTrue(q.isPublished());
            assertEquals("v1", q.getVersion());
            assertEquals(nodes, q.getNodes());
            assertEquals(now, q.getCreatedAt());
            assertEquals(now, q.getUpdatedAt());
        }

        @Test
        @DisplayName("throws NPE when id is null")
        void throwsOnNullId() {
            // Given
            List<QuestNode> nodes = List.of(n(1));
            Instant now = Instant.now();
            // When / Then
            assertThrows(NullPointerException.class,
                    () -> new CustomQuest(null, "owner", "name", 1, nodes, false, "v", now, now));
        }

        @Test
        @DisplayName("throws NPE when ownerId is null")
        void throwsOnNullOwner() {
            // Given
            List<QuestNode> nodes = List.of(n(1));
            Instant now = Instant.now();
            // When / Then
            assertThrows(NullPointerException.class,
                    () -> new CustomQuest("id", null, "name", 1, nodes, false, "v", now, now));
        }

        @Test
        @DisplayName("throws NPE when name is null")
        void throwsOnNullName() {
            // Given
            List<QuestNode> nodes = List.of(n(1));
            Instant now = Instant.now();
            // When / Then
            assertThrows(NullPointerException.class,
                    () -> new CustomQuest("id", "owner", null, 1, nodes, false, "v", now, now));
        }

        @Test
        @DisplayName("throws NPE when nodes is null")
        void throwsOnNullNodes() {
            // Given
            Instant now = Instant.now();
            // When / Then
            assertThrows(NullPointerException.class,
                    () -> new CustomQuest("id", "owner", "name", 1, null, false, "v", now, now));
        }

        @Test
        @DisplayName("throws NPE when createdAt is null")
        void throwsOnNullCreatedAt() {
            // Given
            List<QuestNode> nodes = List.of(n(1));
            Instant now = Instant.now();
            // When / Then
            assertThrows(NullPointerException.class,
                    () -> new CustomQuest("id", "owner", "name", 1, nodes, false, "v", null, now));
        }

        @Test
        @DisplayName("throws NPE when updatedAt is null")
        void throwsOnNullUpdatedAt() {
            // Given
            List<QuestNode> nodes = List.of(n(1));
            Instant now = Instant.now();
            // When / Then
            assertThrows(NullPointerException.class,
                    () -> new CustomQuest("id", "owner", "name", 1, nodes, false, "v", now, null));
        }

        @Test
        @DisplayName("sets version='' when version is null")
        void versionBecomesEmptyWhenNull() {
            // Given
            List<QuestNode> nodes = List.of(n(1));
            Instant now = Instant.now();
            // When
            CustomQuest q = new CustomQuest("id", "owner", "name", 1, nodes, false, null, now, now);
            // Then
            assertEquals("", q.getVersion());
        }

        @Test
        @DisplayName("defensive copy of nodes when created then original changes don't affect")
        void defensiveCopyOnCtor() {
            // Given
            List<QuestNode> original = new ArrayList<>(List.of(n(1)));
            Instant now = Instant.now();
            CustomQuest q = new CustomQuest("id", "owner", "name", 1, original, false, "v", now, now);
            // When
            original.add(n(2));
            // Then
            assertEquals(1, q.getNodes().size());
        }

        @Test
        @DisplayName("nodes list is unmodifiable when accessed then UOE")
        void nodesIsUnmodifiable() {
            // Given
            List<QuestNode> nodes = List.of(n(1));
            Instant now = Instant.now();
            CustomQuest q = new CustomQuest("id", "owner", "name", 1, nodes, false, "v", now, now);
            // When / Then
            //noinspection DataFlowIssue
            assertThrows(UnsupportedOperationException.class, () -> q.getNodes().add(n(2)));
        }
    }

    @Nested
    @DisplayName("withUpdate()")
    class WithUpdate {

        @Test
        @DisplayName("updates nodes/startId/published/version when called then changed")
        void updatesFields() {
            // Given
            Instant t0 = Instant.now();
            CustomQuest q0 = new CustomQuest("id", "owner", "name", 1, List.of(n(1)), false, "v1", t0, t0);
            // When
            CustomQuest q1 = q0.withUpdate(List.of(n(2), n(3)), 2, true, "v2");
            // Then
            assertEquals(2, q1.getStartId());
            assertTrue(q1.isPublished());
            assertEquals("v2", q1.getVersion());
            assertEquals(2, q1.getNodes().size());
            assertEquals(List.of(2, 3), q1.getNodes().stream().map(QuestNode::getId).toList());
        }

        @Test
        @DisplayName("preserves id/owner/name/createdAt when updated then same")
        void preservesIdentityFields() {
            // Given
            Instant t0 = Instant.now();
            CustomQuest q0 = new CustomQuest("ID", "OWN", "NAME", 1, List.of(n(1)), false, "v1", t0, t0);
            // When
            CustomQuest q1 = q0.withUpdate(List.of(n(2)), 2, true, "v2");
            // Then
            assertEquals("ID", q1.getId());
            assertEquals("OWN", q1.getOwnerId());
            assertEquals("NAME", q1.getName());
            assertEquals(t0, q1.getCreatedAt());
        }

        @Test
        @DisplayName("bumps updatedAt to now when updated then > old")
        void bumpsUpdatedAt() {
            // Given
            Instant t0 = Instant.now();
            CustomQuest q0 = new CustomQuest("id", "owner", "name", 1, List.of(n(1)), false, "v1", t0, t0);
            // When
            CustomQuest q1 = q0.withUpdate(List.of(n(2)), 2, true, "v2");
            // Then
            assertFalse(q1.getUpdatedAt().isBefore(t0));
        }

        @Test
        @DisplayName("defensive copy on new nodes when updated then unmodifiable")
        void defensiveCopyOnUpdate() {
            // Given
            Instant t0 = Instant.now();
            CustomQuest q0 = new CustomQuest("id", "owner", "name", 1, List.of(n(1)), false, "v1", t0, t0);
            // When
            CustomQuest q1 = q0.withUpdate(new ArrayList<>(List.of(n(2))), 2, true, "v2");
            // Then
            assertThrows(UnsupportedOperationException.class, () -> q1.getNodes().add(n(99)));
        }
    }
}