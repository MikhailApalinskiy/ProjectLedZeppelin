package com.javarush.apalinskiy.repository.inmemory.quest;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryCustomQuestRepository")
class InMemoryCustomQuestRepositoryTest {

    private InMemoryCustomQuestRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryCustomQuestRepository();
    }

    private QuestNode nf(int id) {
        return QuestNode.nonFin(id, "n" + id, List.of(new Option("go", id + 1)), null);
    }

    private List<QuestNode> nodes(int... ids) {
        return Arrays.stream(ids).mapToObj(this::nf).collect(Collectors.toList());
    }

    @Nested
    @DisplayName("create / get / listAll / listByOwner")
    class CreateGetList {

        @Test
        @DisplayName("create then appears in listByOwner when requested then size=1")
        void createAppearsForOwner() {
            // Given
            String owner = "alice";
            // When
            repo.create(owner, "Q1", 1, nodes(1, 2), false, "v1");
            // Then
            assertEquals(1, repo.listByOwner(owner).size());
        }

        @Test
        @DisplayName("listByOwner filters by owner when mixed owners then only matching")
        void listByOwnerFilters() {
            // Given
            repo.create("alice", "Q1", 1, nodes(1), false, "v1");
            repo.create("bob", "Q2", 1, nodes(1), false, "v1");
            // When
            List<CustomQuest> list = repo.listByOwner("alice");
            // Then
            assertEquals(1, list.size());
            assertEquals("alice", list.getFirst().getOwnerLogin());
        }

        @Test
        @DisplayName("listAll sorted by updatedAt DESC when one updated then it is first")
        void listAllSortedByUpdatedAt() {
            // Given
            repo.create("o", "A", 1, nodes(1), false, "v1");
            repo.create("o", "B", 1, nodes(1), false, "v1");
            List<CustomQuest> all = repo.listAll();
            String idA = all.stream().filter(q -> q.getName().equals("A")).findFirst().orElseThrow().getId();
            String idB = all.stream().filter(q -> q.getName().equals("B")).findFirst().orElseThrow().getId();
            // When
            repo.update(idA, 2, nodes(2), true, "v2");
            List<CustomQuest> after = repo.listAll();
            // Then
            assertEquals(idA, after.get(0).getId());
            assertEquals(idB, after.get(1).getId());
        }

        @Test
        @DisplayName("get returns present when id exists then ok")
        void getReturnsPresent() {
            // Given
            repo.create("o", "X", 1, nodes(1), false, "v1");
            String id = repo.listAll().getFirst().getId();
            // When / Then
            assertTrue(repo.get(id).isPresent());
        }
    }

    @Nested
    @DisplayName("update / delete / deleteIfOwner")
    class UpdateDelete {

        @Test
        @DisplayName("deleteIfOwner returns false when id missing then no changes")
        void deleteIfOwnerMissingId() {
            // Given
            repo.create("alice", "Q", 1, nodes(1), false, "v1");
            int before = repo.listAll().size();
            // When
            boolean res = repo.deleteIfOwner("no-such-id", "alice");
            // Then
            assertFalse(res);
            assertEquals(before, repo.listAll().size());
        }

        @Test
        @DisplayName("update changes fields and bumps updatedAt when called then updated")
        void updateChangesAndBumpsTime() {
            // Given
            repo.stageCreate("own", "Q", 1, nodes(1), "v1");
            String pid = repo.listPendingNew().getFirst().getPendingId();
            String id = repo.approveCreate(pid);
            Instant before = repo.get(id).orElseThrow().getUpdatedAt();
            // When
            repo.update(id, 2, nodes(2, 3), true, "v2");
            // Then
            CustomQuest q = repo.get(id).orElseThrow();
            assertEquals(2, q.getStartId());
            assertTrue(q.isPublished());
            assertEquals("v2", q.getVersion());
            assertFalse(q.getUpdatedAt().isBefore(before));
            assertEquals(2, q.getNodes().size());
        }

        @Test
        @DisplayName("delete removes quest when existing then true")
        void deleteRemoves() {
            // Given
            repo.create("o", "Q", 1, nodes(1), false, "v1");
            String id = repo.listAll().getFirst().getId();
            // When / Then
            assertTrue(repo.delete(id));
            assertTrue(repo.listAll().isEmpty());
        }

        @Test
        @DisplayName("delete returns false when id missing then false")
        void deleteMissing() {
            // Given / When / Then
            assertFalse(repo.delete("nope"));
        }

        @Test
        @DisplayName("deleteIfOwner true when owner matches then removed")
        void deleteIfOwnerWhenMatches() {
            // Given
            repo.create("alice", "Q", 1, nodes(1), false, "v");
            String id = repo.listByOwner("alice").getFirst().getId();
            // When / Then
            assertTrue(repo.deleteIfOwner(id, "alice"));
            assertTrue(repo.listByOwner("alice").isEmpty());
        }

        @Test
        @DisplayName("deleteIfOwner false when owner differs then not removed")
        void deleteIfOwnerWhenDiffers() {
            // Given
            repo.create("alice", "Q", 1, nodes(1), false, "v");
            String id = repo.listByOwner("alice").getFirst().getId();
            // When / Then
            assertFalse(repo.deleteIfOwner(id, "bob"));
            assertTrue(repo.get(id).isPresent());
        }
    }

    @Nested
    @DisplayName("stageCreate / listPendingNew / approveCreate / rejectCreate")
    class StageCreateFlow {

        @Test
        @DisplayName("stageCreate adds pending then listPendingNew sorted DESC")
        void stageCreateAddsAndSorts() throws InterruptedException {
            // Given
            repo.stageCreate("a", "Q1", 1, nodes(1), "v1");
            Thread.sleep(2); // ensure later submittedAt
            repo.stageCreate("a", "Q2", 1, nodes(1), "v2");
            // When
            List<CustomQuestRepository.PendingNew> list = repo.listPendingNew();
            // Then
            assertEquals("Q2", list.get(0).getName());
            Instant t0 = list.get(0).getSubmittedAt();
            Instant t1 = list.get(1).getSubmittedAt();
            assertFalse(t0.isBefore(t1));
        }

        @Test
        @DisplayName("approveCreate returns new id and publishes quest then present")
        void approveCreatePublishes() {
            // Given
            repo.stageCreate("o", "Q", 1, nodes(1), "v");
            String pendingId = repo.listPendingNew().getFirst().getPendingId();
            // When
            String newId = repo.approveCreate(pendingId);
            // Then
            CustomQuest q = repo.get(newId).orElseThrow();
            assertTrue(q.isPublished());
            assertEquals("Q", q.getName());
            assertTrue(repo.listPendingNew().isEmpty());
        }

        @Test
        @DisplayName("rejectCreate removes pending when exists then list empty")
        void rejectCreateRemoves() {
            // Given
            repo.stageCreate("o", "Q", 1, nodes(1), "v");
            String pid = repo.listPendingNew().getFirst().getPendingId();
            // When
            repo.rejectCreate(pid);
            // Then
            assertTrue(repo.listPendingNew().isEmpty());
        }

        @Test
        @DisplayName("approveCreate throws when id missing then NoSuchElementException")
        void approveCreateThrowsOnMissing() {
            // Given / When / Then
            assertThrows(NoSuchElementException.class, () -> repo.approveCreate("nope"));
        }

        @Test
        @DisplayName("rejectCreate throws when id missing then NoSuchElementException")
        void rejectCreateThrowsOnMissing() {
            // Given / When / Then
            assertThrows(NoSuchElementException.class, () -> repo.rejectCreate("nope"));
        }
    }

    @Nested
    @DisplayName("stageEdit / listPendingEdits / approveEdit / rejectEdit")
    class StageEditFlow {

        @Test
        @DisplayName("stageEdit throws when quest missing then NoSuchElementException")
        void stageEditThrowsWhenMissing() {
            // Given / When / Then
            assertThrows(NoSuchElementException.class, () -> repo.stageEdit("nope", 2, nodes(2), "v2"));
        }

        @Test
        @DisplayName("stageEdit overwrites previous for same quest then latest kept")
        void stageEditOverwritesPrevious() throws InterruptedException {
            // Given
            repo.stageCreate("o", "Q", 1, nodes(1), "v1");
            String id = repo.approveCreate(repo.listPendingNew().getFirst().getPendingId());
            repo.stageEdit(id, 2, nodes(2), "v2");
            Instant first = repo.listPendingEdits().getFirst().getSubmittedAt();
            Thread.sleep(2);
            // When
            repo.stageEdit(id, 3, nodes(3), "v3");
            // Then
            List<CustomQuestRepository.PendingEdit> list = repo.listPendingEdits();
            assertEquals(1, list.size());
            assertEquals(3, list.getFirst().getStartId());
            assertFalse(list.getFirst().getSubmittedAt().isBefore(first));
        }

        @Test
        @DisplayName("listPendingEdits sorted DESC across different quests")
        void listPendingEditsSortedAcrossQuests() throws InterruptedException {
            // Given
            repo.stageCreate("o", "Q1", 1, nodes(1), "v1");
            String id1 = repo.approveCreate(repo.listPendingNew().getFirst().getPendingId());
            repo.stageCreate("o", "Q2", 1, nodes(1), "v1");
            String id2 = repo.approveCreate(repo.listPendingNew().getFirst().getPendingId());
            repo.stageEdit(id1, 2, nodes(2), "v2");
            Thread.sleep(2);
            repo.stageEdit(id2, 3, nodes(3), "v3");
            // When
            List<CustomQuestRepository.PendingEdit> list = repo.listPendingEdits();
            // Then
            assertEquals(2, list.size());
            Instant t0 = list.get(0).getSubmittedAt();
            Instant t1 = list.get(1).getSubmittedAt();
            assertFalse(t0.isBefore(t1));
            assertEquals(3, list.get(0).getStartId());
        }

        @Test
        @DisplayName("approveEdit applies pending edit then quest updated and pending cleared")
        void approveEditApplies() {
            // Given
            repo.stageCreate("o", "Q", 1, nodes(1), "v1");
            String id = repo.approveCreate(repo.listPendingNew().getFirst().getPendingId());
            Instant before = repo.get(id).orElseThrow().getUpdatedAt();
            repo.stageEdit(id, 5, nodes(5, 6), "v2");
            // When
            repo.approveEdit(id);
            // Then
            CustomQuest q = repo.get(id).orElseThrow();
            assertEquals(5, q.getStartId());
            assertEquals("v2", q.getVersion());
            assertEquals(2, q.getNodes().size());
            assertFalse(q.getUpdatedAt().isBefore(before));
            assertTrue(repo.listPendingEdits().isEmpty());
        }

        @Test
        @DisplayName("rejectEdit removes pending when exists then empty")
        void rejectEditRemoves() {
            // Given
            repo.stageCreate("o", "Q", 1, nodes(1), "v1");
            String id = repo.approveCreate(repo.listPendingNew().getFirst().getPendingId());
            repo.stageEdit(id, 2, nodes(2), "v2");
            // When
            repo.rejectEdit(id);
            // Then
            assertTrue(repo.listPendingEdits().isEmpty());
        }

        @Test
        @DisplayName("approveEdit throws when pending missing then NoSuchElementException")
        void approveEditThrowsWhenMissing() {
            // Given / When / Then
            assertThrows(NoSuchElementException.class, () -> repo.approveEdit("nope"));
        }

        @Test
        @DisplayName("rejectEdit throws when pending missing then NoSuchElementException")
        void rejectEditThrowsWhenMissing() {
            // Given / When / Then
            assertThrows(NoSuchElementException.class, () -> repo.rejectEdit("nope"));
        }
    }
}