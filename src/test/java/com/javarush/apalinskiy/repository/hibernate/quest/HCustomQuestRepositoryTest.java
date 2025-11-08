package com.javarush.apalinskiy.repository.hibernate.quest;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import com.javarush.apalinskiy.utils.HibernateUtil;
import it.AbstractDbTest;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HCustomQuestRepository (IT)")
class HCustomQuestRepositoryIT extends AbstractDbTest {

    private HCustomQuestRepository sut;
    private SessionFactory sf;

    private static MockedStatic<HibernateUtil> UTIL;

    private static final String ADMIN_ID = "11111111-2222-3333-4444-555555555555";
    private static final String ALICE_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String SEEDED_QUEST_ID = "99999999-1111-2222-3333-444444444444";

    private QuestNode node(int id, String text, boolean fin, String image, Option... opts) {
        List<Option> list = new ArrayList<>();
        Collections.addAll(list, opts);
        return QuestNode.of(id, text, list, fin, image);
    }

    private Option opt(String choice, Integer next) {
        return new Option(choice, next);
    }

    private CustomQuest getWithGraph(String id) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            CustomQuest q = s.get(CustomQuest.class, id);
            if (q != null) {
                Hibernate.initialize(q.getNodes());
                q.getNodes().forEach(n -> Hibernate.initialize(n.getOptions()));
            }
            s.getTransaction().commit();
            return q;
        }
    }

    private List<QuestNode> simpleGraph() {
        return List.of(
                node(1, "Start", false, "a.png", opt("Go", 2)),
                node(2, "End", true, null)
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T getInNewTx(Object id) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            T found = s.get((Class<T>) CustomQuest.class, id);
            s.getTransaction().commit();
            return found;
        }
    }

    @BeforeAll
    static void staticInit() {
    }

    @AfterAll
    static void staticClose() {
        if (UTIL != null) UTIL.close();
    }

    @BeforeEach
    void initRepo() {
        this.sf = super.sessionFactory;
        UTIL = Mockito.mockStatic(HibernateUtil.class);
        UTIL.when(HibernateUtil::getSessionFactory).thenReturn(this.sf);
        this.sut = new HCustomQuestRepository();
        assertNotNull(sut);
    }

    @AfterEach
    void tearDownRepo() {
        if (UTIL != null) {
            UTIL.close();
            UTIL = null;
        }
    }

    @Nested
    @DisplayName("get(id)")
    class GetById {

        @Test
        @DisplayName("Given seeded quest id — When get — Then returns quest with initialized graph")
        void getSeededQuest() {
            // Given
            // When
            Optional<CustomQuest> out = sut.get(SEEDED_QUEST_ID);
            // Then
            assertTrue(out.isPresent(), "Seeded quest should be found");
            CustomQuest cq = out.get();
            assertEquals("Demo Quest", cq.getName());
            assertEquals(CustomQuest.ModerationStatus.LIVE, cq.getModerationStatus());
            assertNotNull(cq.getNodes());
            assertEquals(2, cq.getNodes().size(), "Expected exactly 2 nodes from seed");
            var node1 = cq.getNodes().stream().filter(n -> n.getId() == 1).findFirst().orElseThrow();
            var node2 = cq.getNodes().stream().filter(n -> n.getId() == 2).findFirst().orElseThrow();
            assertFalse(node1.getFin());
            assertTrue(node2.getFin());
            assertEquals(1, node1.getOptions().size());
            assertEquals("Идти дальше", node1.getOptions().getFirst().getChoice());
            assertEquals(2, node1.getOptions().getFirst().getNext());
        }
    }

    @Nested
    @DisplayName("countAllLive(q)")
    class CountAllLive {

        @Test
        @DisplayName("Given empty filter — When countAllLive — Then includes seeded quest")
        void countIncludesSeeded() {
            // Given
            // When
            int count = sut.countAllLive(null);
            // Then
            assertTrue(count >= 1, "At least 1 live quest expected (seeded)");
        }

        @Test
        @DisplayName("Given name filter 'demo' — When countAllLive — Then matches seeded quest case-insensitively")
        void countByNameFilter() {
            // Given
            String filter = "demo";
            // When
            int count = sut.countAllLive(filter);
            // Then
            assertTrue(count >= 1, "Seeded 'Demo Quest' must be counted by filter 'demo'");
        }
    }

    @Nested
    @DisplayName("findAllLivePaged(page,size,q)")
    class FindAllLivePaged {

        @Test
        @DisplayName("Given filter 'demo' — When findAllLivePaged — Then returns seeded quest with author and nodes init")
        void pagedIncludesSeeded() {
            // Given
            int page = 1, size = 10;
            String filter = "demo";
            // When
            List<CustomQuest> list = sut.findAllLivePaged(page, size, filter);
            // Then
            assertFalse(list.isEmpty(), "Expected seeded quest in page");
            CustomQuest cq = list.stream()
                    .filter(q -> SEEDED_QUEST_ID.equals(q.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Seeded quest not found in page"));
            assertEquals("Demo Quest", cq.getName());
            assertNotNull(cq.getUser());
            assertEquals(ADMIN_ID, cq.getUser().getUserId());
            assertNotNull(cq.getNodes());
            assertEquals(2, cq.getNodes().size());
        }
    }

    @Nested
    @DisplayName("countByOwner / findByOwnerPaged")
    class ByOwner {

        @Test
        @DisplayName("Given owner=ADMIN_ID — When countByOwner onlyLive=false — Then >= 1 (seeded)")
        void countByOwnerIncludesSeeded() {
            // Given
            // When
            int count = sut.countByOwner(ADMIN_ID, null, false);
            // Then
            assertTrue(count >= 1, "Owner must have at least 1 quest (seed)");
        }

        @Test
        @DisplayName("Given owner=ADMIN_ID — When findByOwnerPaged — Then contains seeded quest")
        void findByOwnerPagedContainsSeeded() {
            // Given
            // When
            List<CustomQuest> list = sut.findByOwnerPaged(ADMIN_ID, 1, 10, null, false);
            // Then
            assertFalse(list.isEmpty());
            boolean found = false;
            for (int p = 1; p <= 10 && !found; p++) {
                var page = sut.findByOwnerPaged(ADMIN_ID, p, 10, null, true);
                if (page.isEmpty()) break;
                found = page.stream().anyMatch(q -> SEEDED_QUEST_ID.equals(q.getId()));
            }
            assertTrue(found, "Seeded quest must be present for owner " + ADMIN_ID);
        }
    }

    @Nested
    @DisplayName("create(ownerId,name,startId,nodes,published,versionNote)")
    class CreateMethod {

        @Test
        @DisplayName("Given existing owner; When published=true; Then quest persisted LIVE with graph attached")
        void createsLiveWithGraph() {
            // Given
            List<QuestNode> graph = simpleGraph();
            // When
            sut.create(ADMIN_ID, "Demo Created", 1, graph, true, "v1");
            // Then
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                List<CustomQuest> found = s.createQuery(
                                "from CustomQuest q where q.user.userId = :uid and q.name = :nm",
                                CustomQuest.class)
                        .setParameter("uid", ADMIN_ID)
                        .setParameter("nm", "Demo Created")
                        .list();
                s.getTransaction().commit();
                assertEquals(1, found.size());
                CustomQuest q = found.getFirst();
                assertEquals(CustomQuest.ModerationStatus.LIVE, q.getModerationStatus());
                assertEquals(1, q.getStartId());
                assertEquals("v1", q.getVersion());
                Hibernate.initialize(q.getNodes());
                assertEquals(2, q.getNodes().size());
                assertEquals("Start", q.getNodes().getFirst().getText());
                assertEquals(1, q.getNodes().getFirst().getId());
            }
        }

        @Test
        @DisplayName("Given missing owner; When create; Then throws IllegalArgumentException")
        void throwsWhenOwnerMissing() {
            // Given
            String missingOwner = "no-such-user";
            List<QuestNode> graph = simpleGraph();
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.create(missingOwner, "X", 1, graph, false, "note"));
        }
    }

    @Nested
    @DisplayName("get(id)")
    class GetMethod {

        @Test
        @DisplayName("Given existing quest; When get; Then returns Optional with initialized user and nodes")
        void returnsQuestWithGraph() {
            // Given
            List<QuestNode> graph = simpleGraph();
            sut.create(ADMIN_ID, "Get Me", 1, graph, true, "v1");
            String questId;
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                questId = s.createQuery(
                                "select q.id from CustomQuest q where q.user.userId=:uid and q.name=:nm",
                                String.class)
                        .setParameter("uid", ADMIN_ID)
                        .setParameter("nm", "Get Me")
                        .uniqueResult();
                s.getTransaction().commit();
            }
            assertNotNull(questId);
            // When
            Optional<CustomQuest> got = sut.get(questId);
            // Then
            assertTrue(got.isPresent());
            CustomQuest q = got.get();
            assertNotNull(q.getUser());
            assertEquals(ADMIN_ID, q.getUser().getUserId());
            assertNotNull(q.getNodes());
            assertEquals(2, q.getNodes().size());
            assertEquals("Start", q.getNodes().getFirst().getText());
        }

        @Test
        @DisplayName("Given no quest; When get; Then returns Optional.empty")
        void returnsEmpty() {
            // Given
            String unknown = "no-quest-id";
            // When
            Optional<CustomQuest> got = sut.get(unknown);
            // Then
            assertTrue(got.isEmpty());
        }
    }

    @Nested
    @DisplayName("update(id,startId,nodes,published,versionNote)")
    class UpdateMethod {

        @Test
        @DisplayName("Given existing quest; When published=false; Then status=PENDING_EDIT; graph replaced")
        void updatesToPendingEdit() {
            // Given
            sut.create(ADMIN_ID, "Upd", 1, simpleGraph(), true, "v1");
            String qid;
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                qid = s.createQuery("select q.id from CustomQuest q where q.name=:n", String.class)
                        .setParameter("n", "Upd")
                        .uniqueResult();
                s.getTransaction().commit();
            }
            List<QuestNode> newGraph = List.of(
                    node(10, "Root", false, null, opt("Next", 11)),
                    node(11, "Final", true, null)
            );
            // When
            sut.update(qid, 10, newGraph, false, "v2");
            // Then
            CustomQuest updated = getWithGraph(qid);
            assertEquals(CustomQuest.ModerationStatus.PENDING_EDIT, updated.getModerationStatus());
            assertEquals(10, updated.getStartId());
            assertEquals("v2", updated.getVersion());
            assertEquals(2, updated.getNodes().size());
            assertEquals(10, updated.getNodes().getFirst().getId());
        }

        @Test
        @DisplayName("Given missing quest; When update; Then returns gracefully (no exception)")
        void updateMissing() {
            // Given
            String missing = "missing";
            List<QuestNode> graph = simpleGraph();
            // When / Then
            assertDoesNotThrow(() -> sut.update(missing, 1, graph, true, "nope"));
        }
    }

    @Nested
    @DisplayName("delete(id)")
    class DeleteMethod {

        @Test
        @DisplayName("Given existing quest; When delete; Then removes quest and pending edit if exists")
        void deletesQuestAndPending() {
            // Given
            sut.create(ADMIN_ID, "ToDelete", 1, simpleGraph(), true, "v");
            String qid;
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                qid = s.createQuery("select q.id from CustomQuest q where q.name=:n", String.class)
                        .setParameter("n", "ToDelete")
                        .uniqueResult();
                s.getTransaction().commit();
            }
            sut.stageEdit(qid, 1, simpleGraph(), "edit-1");
            // When
            boolean deleted = sut.delete(qid);
            // Then
            assertTrue(deleted);
            CustomQuest gone = getInNewTx(qid);
            assertNull(gone);
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                Long cnt = s.createQuery("select count(p) from PendingEditRow p where p.questId=:id", Long.class)
                        .setParameter("id", qid).uniqueResult();
                s.getTransaction().commit();
                assertEquals(0L, cnt);
            }
        }

        @Test
        @DisplayName("Given unknown id; When delete; Then returns false")
        void deleteUnknown() {
            // Given
            String missing = "missing";
            // When
            boolean deleted = sut.delete(missing);
            // Then
            assertFalse(deleted);
        }
    }

    @Nested
    @DisplayName("deleteIfOwner(id,ownerId)")
    class DeleteIfOwnerMethod {

        @Test
        @DisplayName("Given owner matches; When deleteIfOwner; Then deletes quest")
        void deletesIfOwnerMatches() {
            // Given
            sut.create(ALICE_ID, "Own", 1, simpleGraph(), true, "v");
            String qid;
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                qid = s.createQuery("select q.id from CustomQuest q where q.name=:n", String.class)
                        .setParameter("n", "Own").uniqueResult();
                s.getTransaction().commit();
            }
            // When
            boolean ok = sut.deleteIfOwner(qid, ALICE_ID);
            // Then
            assertTrue(ok);
            assertNull(getInNewTx(qid));
        }

        @Test
        @DisplayName("Given owner mismatch; When deleteIfOwner; Then returns false and stays")
        void deniesOnMismatch() {
            // Given
            sut.create(ALICE_ID, "Own2", 1, simpleGraph(), true, "v");
            String qid;
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                qid = s.createQuery("select q.id from CustomQuest q where q.name=:n", String.class)
                        .setParameter("n", "Own2").uniqueResult();
                s.getTransaction().commit();
            }
            // When
            boolean ok = sut.deleteIfOwner(qid, ADMIN_ID);
            // Then
            assertFalse(ok);
            assertNotNull(getInNewTx(qid));
        }
    }

    @Nested
    @DisplayName("stageCreate / listPendingNew / approveCreate / rejectCreate")
    class CreateFlow {

        @Test
        @DisplayName("Given staged create; When listPendingNew; Then returns DTO with parsed graph")
        void listPendingNewShows() {
            // Given
            sut.stageCreate(ALICE_ID, "PN", 1, simpleGraph(), "vX");
            // When
            List<CustomQuestRepository.PendingNew> list = sut.listPendingNew();
            // Then
            assertFalse(list.isEmpty());
            CustomQuestRepository.PendingNew top = list.getFirst();
            assertEquals("PN", top.getName());
            assertEquals(1, top.getStartId());
            assertEquals(ALICE_ID, top.getOwnerId());
            assertEquals(2, top.getNodes().size());
        }

        @Test
        @DisplayName("Given staged create; When approveCreate; Then materializes quest and removes row")
        void approveCreateWorks() {
            // Given
            sut.stageCreate(ADMIN_ID, "ToApprove", 1, simpleGraph(), "v2");
            String pendingId;
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                pendingId = s.createQuery("select p.pendingId from PendingNewRow p where p.name=:n",
                                String.class)
                        .setParameter("n", "ToApprove").uniqueResult();
                s.getTransaction().commit();
            }
            assertNotNull(pendingId);
            // When
            String questId = sut.approveCreate(pendingId);
            // Then
            assertNotNull(questId);
            CustomQuest q = getInNewTx(questId);
            assertNotNull(q);
            assertEquals("ToApprove", q.getName());
            assertEquals(CustomQuest.ModerationStatus.LIVE, q.getModerationStatus());
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                Long left = s.createQuery("select count(p) from PendingNewRow p where p.pendingId=:id",
                                Long.class)
                        .setParameter("id", pendingId).uniqueResult();
                s.getTransaction().commit();
                assertEquals(0L, left);
            }
        }

        @Test
        @DisplayName("Given staged create; When rejectCreate; Then only removes pending row")
        void rejectCreateWorks() {
            // Given
            sut.stageCreate(ALICE_ID, "ToReject", 1, simpleGraph(), "vR");
            String pendingId;
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                pendingId = s.createQuery("select p.pendingId from PendingNewRow p where p.name=:n",
                                String.class)
                        .setParameter("n", "ToReject").uniqueResult();
                s.getTransaction().commit();
            }
            // When
            sut.rejectCreate(pendingId);
            // Then
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                Long left = s.createQuery("select count(p) from PendingNewRow p where p.pendingId=:id",
                                Long.class)
                        .setParameter("id", pendingId).uniqueResult();
                s.getTransaction().commit();
                assertEquals(0L, left);
            }
        }
    }

    @Nested
    @DisplayName("stageEdit / listPendingEdits / approveEdit / rejectEdit")
    class EditFlow {

        @Test
        @DisplayName("Given live quest; When stageEdit; Then quest->PENDING_EDIT and row upserted")
        void stageEditCreatesRow() {
            // Given
            sut.create(ADMIN_ID, "LiveQ", 1, simpleGraph(), true, "v1");
            String qid;
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                qid = s.createQuery("select q.id from CustomQuest q where q.name=:n", String.class)
                        .setParameter("n", "LiveQ").uniqueResult();
                s.getTransaction().commit();
            }
            List<QuestNode> newGraph = List.of(
                    node(5, "R", false, null, opt("N", 6)),
                    node(6, "F", true, null)
            );
            // When
            sut.stageEdit(qid, 5, newGraph, "v2");
            // Then
            CustomQuest after = getInNewTx(qid);
            assertEquals(CustomQuest.ModerationStatus.PENDING_EDIT, after.getModerationStatus());
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                Long cnt = s.createQuery("select count(p) from PendingEditRow p where p.questId=:id",
                                Long.class)
                        .setParameter("id", qid).uniqueResult();
                s.getTransaction().commit();
                assertEquals(1L, cnt);
            }
        }

        @Test
        @DisplayName("Given pending edit; When listPendingEdits; Then returns DTOs sorted by submittedAt desc")
        void listPendingEditsWorks() {
            // Given
            sut.create(ALICE_ID, "E1", 1, simpleGraph(), true, "v");
            sut.create(ALICE_ID, "E2", 1, simpleGraph(), true, "v");
            String id1;
            String id2;
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                id1 = s.createQuery("select q.id from CustomQuest q where q.name=:n", String.class)
                        .setParameter("n", "E1").uniqueResult();
                id2 = s.createQuery("select q.id from CustomQuest q where q.name=:n", String.class)
                        .setParameter("n", "E2").uniqueResult();
                s.getTransaction().commit();
            }
            sut.stageEdit(id1, 1, simpleGraph(), "v1");
            sut.stageEdit(id2, 1, simpleGraph(), "v2");
            // When
            List<CustomQuestRepository.PendingEdit> list = sut.listPendingEdits();
            // Then
            assertTrue(list.size() >= 2);
            assertNotNull(list.get(0).getSubmittedAt());
            assertNotNull(list.get(1).getSubmittedAt());
            var names = list.stream().map(CustomQuestRepository.PendingEdit::getName).toList();
            assertTrue(names.contains("E1"));
            assertTrue(names.contains("E2"));
        }

        @Test
        @DisplayName("Given pending edit; When approveEdit; Then graph replaced, status LIVE, pending removed")
        void approveEditWorks() {
            // Given
            sut.create(ADMIN_ID, "AE", 1, simpleGraph(), true, "v1");
            String qid;
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                qid = s.createQuery("select q.id from CustomQuest q where q.name=:n", String.class)
                        .setParameter("n", "AE").uniqueResult();
                s.getTransaction().commit();
            }
            List<QuestNode> newGraph = List.of(
                    node(10, "Root", false, null, opt("Next", 11)),
                    node(11, "Final", true, null)
            );
            sut.stageEdit(qid, 10, newGraph, "v2");
            // When
            sut.approveEdit(qid);
            // Then
            CustomQuest q = getWithGraph(qid);
            assertEquals(CustomQuest.ModerationStatus.LIVE, q.getModerationStatus());
            assertEquals(10, q.getStartId());
            Hibernate.initialize(q.getNodes());
            assertEquals(2, q.getNodes().size());
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                Long cnt = s.createQuery("select count(p) from PendingEditRow p where p.questId=:id",
                                Long.class)
                        .setParameter("id", qid).uniqueResult();
                s.getTransaction().commit();
                assertEquals(0L, cnt);
            }
        }

        @Test
        @DisplayName("Given pending edit; When rejectEdit; Then pending removed and status LIVE restored")
        void rejectEditWorks() {
            // Given
            sut.create(ALICE_ID, "REJ", 1, simpleGraph(), true, "v1");
            String qid;
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                qid = s.createQuery("select q.id from CustomQuest q where q.name=:n", String.class)
                        .setParameter("n", "REJ").uniqueResult();
                s.getTransaction().commit();
            }
            sut.stageEdit(qid, 1, simpleGraph(), "vX");
            // When
            sut.rejectEdit(qid);
            // Then
            CustomQuest q = getInNewTx(qid);
            assertEquals(CustomQuest.ModerationStatus.LIVE, q.getModerationStatus());
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                Long cnt = s.createQuery("select count(p) from PendingEditRow p where p.questId=:id",
                                Long.class)
                        .setParameter("id", qid).uniqueResult();
                s.getTransaction().commit();
                assertEquals(0L, cnt);
            }
        }
    }

    @Nested
    @DisplayName("countAllLive(q) / findAllLivePaged(page,size,q)")
    class LiveListing {

        @Test
        @DisplayName("Given live quests; When countAllLive; Then returns total, filtered by name if q present")
        void countAllLiveFilters() {
            // Given
            sut.create(ADMIN_ID, "Alpha", 1, simpleGraph(), true, "v");
            sut.create(ADMIN_ID, "Beta", 1, simpleGraph(), true, "v");
            sut.create(ADMIN_ID, "Gamma", 1, simpleGraph(), true, "v");
            // When / Then
            int total = sut.countAllLive(null);
            assertTrue(total >= 3);
            int filtered = sut.countAllLive("aMm");
            assertTrue(filtered >= 1);
        }

        @Test
        @DisplayName("Given many live quests; When findAllLivePaged; Then returns page ordered by updatedAt desc")
        void findAllLivePagedWorks() {
            // Given
            sut.create(ALICE_ID, "P1", 1, simpleGraph(), true, "v");
            sut.create(ALICE_ID, "P2", 1, simpleGraph(), true, "v");
            sut.create(ALICE_ID, "P3", 1, simpleGraph(), true, "v");
            // When
            List<CustomQuest> page = sut.findAllLivePaged(1, 2, null);
            // Then
            assertEquals(2, page.size());
            assertNotNull(page.get(0).getUser());
            assertNotNull(page.get(1).getUser());
        }
    }

    @Nested
    @DisplayName("countByOwner(ownerId,q,onlyLive) / findByOwnerPaged(ownerId,page,size,q,onlyLive)")
    class OwnerListing {

        @Test
        @DisplayName("Given multiple statuses; When countByOwner onlyLive=false; Then counts all statuses")
        void countByOwnerAllStatuses() {
            // Given
            sut.create(ALICE_ID, "O-Live", 1, simpleGraph(), true, "v");
            sut.stageCreate(ALICE_ID, "O-NewPending", 1, simpleGraph(), "v"); // не создаёт LIVE
            sut.create(ALICE_ID, "O-EditLive", 1, simpleGraph(), true, "v");
            String qid;
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                qid = s.createQuery("select q.id from CustomQuest q where q.name=:n", String.class)
                        .setParameter("n", "O-EditLive").uniqueResult();
                s.getTransaction().commit();
            }
            sut.stageEdit(qid, 1, simpleGraph(), "ev");
            // When
            int cntAll = sut.countByOwner(ALICE_ID, null, false);
            int cntLive = sut.countByOwner(ALICE_ID, null, true);
            // Then
            assertTrue(cntAll >= cntLive);
            assertTrue(cntLive >= 1);
        }

        @Test
        @DisplayName("Given owner quests; When findByOwnerPaged onlyLive=true; Then returns only LIVE with authors")
        void findByOwnerPagedLiveOnly() {
            // Given
            sut.create(ADMIN_ID, "OA1", 1, simpleGraph(), true, "v");
            sut.create(ADMIN_ID, "OA2", 1, simpleGraph(), true, "v");
            // When
            List<CustomQuest> list = sut.findByOwnerPaged(ADMIN_ID, 1, 10, null, true);
            // Then
            assertFalse(list.isEmpty());
            assertTrue(list.stream().allMatch(q -> q.getModerationStatus() == CustomQuest.ModerationStatus.LIVE));
            assertTrue(list.stream().allMatch(q -> q.getUser() != null));
        }

        @Test
        @DisplayName("Given owner quests; When findByOwnerPaged with q filter; Then name LIKE lower(q)")
        void findByOwnerPagedWithFilter() {
            // Given
            sut.create(ALICE_ID, "Searchable", 1, simpleGraph(), true, "v");
            sut.create(ALICE_ID, "NotMatch", 1, simpleGraph(), true, "v");
            // When
            List<CustomQuest> list = sut.findByOwnerPaged(ALICE_ID, 1, 10, "search", true);
            // Then
            assertFalse(list.isEmpty());
            assertTrue(list.stream().allMatch(q -> q.getName().toLowerCase(Locale.ROOT).contains("search")));
        }
    }
}