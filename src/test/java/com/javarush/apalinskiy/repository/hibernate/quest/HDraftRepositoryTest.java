package com.javarush.apalinskiy.repository.hibernate.quest;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.custom.DraftRow;
import it.AbstractDbTest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HDraftRepository (IT) — uses Liquibase test seeds")
class HDraftRepositoryIT extends AbstractDbTest {

    private static final String ALICE_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String SEED_QUEST_ID = "99999999-1111-2222-3333-444444444444";

    private SessionFactory sf;
    private HDraftRepository sut;

    @BeforeEach
    void setSessionFactory() {
        sf = super.sessionFactory;
        this.sut = new HDraftRepository(sf);
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            s.createMutationQuery("delete from DraftRow d where d.ownerId = :uid")
                    .setParameter("uid", ALICE_ID)
                    .executeUpdate();
            tx.commit();
        }
    }

    private static List<QuestNode> demoNodes() {
        List<Option> opt = List.of(new Option("Next", 2));
        QuestNode n1 = QuestNode.of(1, "Start", opt, false, null);
        QuestNode n2 = QuestNode.of(2, "End", List.of(), true, null);
        List<QuestNode> nodes = new ArrayList<>();
        nodes.add(n1);
        nodes.add(n2);
        return nodes;
    }

    private Optional<DraftRow> getDraft(String draftId) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            DraftRow row = s.get(DraftRow.class, draftId);
            tx.commit();
            return Optional.ofNullable(row);
        }
    }

    private List<DraftRow> listDraftsByOwnerOrdered() {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            List<DraftRow> list = s.createQuery("""
                    select d from DraftRow d
                    where d.ownerId = :uid
                    order by d.updatedAt desc
                    """, DraftRow.class)
                    .setParameter("uid", HDraftRepositoryIT.ALICE_ID)
                    .getResultList();
            tx.commit();
            return list;
        }
    }

    @Nested
    @DisplayName("upsertDraft")
    class Upsert {

        @Test
        @DisplayName("creates a new draft for (owner=Alice, target=Demo Quest)")
        void createsNew() {
            // given
            String name = "Demo Draft";
            // when
            sut.upsertDraft(ALICE_ID, SEED_QUEST_ID, name, 1, demoNodes(), "v1");
            // then
            List<DraftRow> list = listDraftsByOwnerOrdered();
            assertEquals(1, list.size());
            DraftRow row = list.getFirst();
            assertEquals(ALICE_ID, row.getOwnerId());
            assertEquals(SEED_QUEST_ID, row.getTargetQuestId());
            assertEquals(name, row.getName());
            assertEquals(1, row.getStartId());
            assertEquals("v1", row.getVersionNote());
            assertNotNull(row.getUpdatedAt());
            assertDoesNotThrow(() -> GraphJsonMapper.fromJson(row.getNodesJson()));
        }

        @Test
        @DisplayName("updates an existing draft (latest version with new name and startId)")
        void updatesLatest() throws InterruptedException {
            // given
            sut.upsertDraft(ALICE_ID, SEED_QUEST_ID, "First", 1, demoNodes(), "v1");
            Thread.sleep(5);
            // when
            sut.upsertDraft(ALICE_ID, SEED_QUEST_ID, "Second", 2, demoNodes(), "v2");
            // then
            List<DraftRow> list = listDraftsByOwnerOrdered();
            assertEquals(1, list.size(), "Upsert should not create multiple rows");
            DraftRow row = list.getFirst();
            assertEquals("Second", row.getName());
            assertEquals(2, row.getStartId());
            assertEquals("v2", row.getVersionNote());
        }

        @Test
        @DisplayName("creates two independent drafts: for target=null and for a specific quest")
        void separatesByTargetKey() {
            // given
            // when
            sut.upsertDraft(ALICE_ID, null, "For New Quest", 0, demoNodes(), null);
            sut.upsertDraft(ALICE_ID, SEED_QUEST_ID, "For Seed Quest", 1, demoNodes(), "seed");
            // then
            List<DraftRow> list = listDraftsByOwnerOrdered();
            assertEquals(2, list.size());
            assertTrue(list.stream().anyMatch(d -> d.getTargetQuestId() == null && d.getName().equals("For New Quest")));
            assertTrue(list.stream().anyMatch(d -> SEED_QUEST_ID.equals(d.getTargetQuestId()) && d.getName().equals("For Seed Quest")));
        }
    }

    @Nested
    @DisplayName("findLatest")
    class FindLatest {

        @Test
        @DisplayName("returns the latest by updatedAt for (owner, target) pair")
        void returnsLatest() throws InterruptedException {
            // given
            sut.upsertDraft(ALICE_ID, SEED_QUEST_ID, "A", 1, demoNodes(), null);
            Thread.sleep(5);
            sut.upsertDraft(ALICE_ID, SEED_QUEST_ID, "B", 2, demoNodes(), null);
            // when
            Optional<DraftRow> latest = sut.findLatest(ALICE_ID, SEED_QUEST_ID);
            // then
            assertTrue(latest.isPresent());
            assertEquals("B", latest.get().getName());
            assertEquals(2, latest.get().getStartId());
        }

        @Test
        @DisplayName("for target=null — separate pool, not mixed with targeted drafts")
        void nullTargetPoolIsSeparate() {
            // given
            sut.upsertDraft(ALICE_ID, null, "X", 0, demoNodes(), null);
            sut.upsertDraft(ALICE_ID, SEED_QUEST_ID, "Y", 1, demoNodes(), null);
            // when
            String nullTargetName = sut.findLatest(ALICE_ID, null).map(DraftRow::getName).orElse(null);
            String targetedName = sut.findLatest(ALICE_ID, SEED_QUEST_ID).map(DraftRow::getName).orElse(null);
            // then
            assertEquals("X", nullTargetName);
            assertEquals("Y", targetedName);
        }
    }

    @Nested
    @DisplayName("countByOwner / findByOwnerPaged")
    class Listing {

        @Test
        @DisplayName("countByOwner counts all owner's drafts (no filter)")
        void countAll() {
            // given
            sut.upsertDraft(ALICE_ID, null, "One", 0, demoNodes(), null);
            sut.upsertDraft(ALICE_ID, SEED_QUEST_ID, "Two", 1, demoNodes(), null);
            // when
            long count = sut.countByOwner(ALICE_ID, null);
            // then
            assertEquals(2, count);
        }

        @Test
        @DisplayName("countByOwner applies case-insensitive name filter")
        void countWithFilter() {
            // given
            sut.upsertDraft(ALICE_ID, null, "alpha draft", 0, demoNodes(), null);
            sut.upsertDraft(ALICE_ID, SEED_QUEST_ID, "Beta", 1, demoNodes(), null);
            // when
            long count = sut.countByOwner(ALICE_ID, "alp");
            // then
            assertEquals(1, count);
        }

        @Test
        @DisplayName("findByOwnerPaged returns list sorted by updatedAt desc and applies filter")
        void pagedAndOrdered() throws InterruptedException {
            // given
            sut.upsertDraft(ALICE_ID, null, "Zed", 0, demoNodes(), null);
            Thread.sleep(5);
            sut.upsertDraft(ALICE_ID, SEED_QUEST_ID, "Alpha", 1, demoNodes(), null);
            // when
            var page1 = sut.findByOwnerPaged(ALICE_ID, 1, 1, null);
            var filtered = sut.findByOwnerPaged(ALICE_ID, 1, 10, "zed");
            // then
            assertEquals(1, page1.size());
            assertEquals("Alpha", page1.getFirst().getName());
            assertEquals(1, filtered.size());
            assertEquals("Zed", filtered.getFirst().getName());
        }
    }

    @Nested
    @DisplayName("createEmpty / rename / delete")
    class Mutations {

        @Test
        @DisplayName("createEmpty creates a minimal draft with [] graph")
        void createEmptyDraft() {
            // when
            sut.createEmpty(ALICE_ID, SEED_QUEST_ID, "Empty");
            // then
            var list = listDraftsByOwnerOrdered();
            assertEquals(1, list.size());
            DraftRow row = list.getFirst();
            assertEquals("Empty", row.getName());
            assertEquals(0, row.getStartId());
            assertEquals("", row.getVersionNote());
            assertEquals("[]", row.getNodesJson());
        }

        @Test
        @DisplayName("rename updates draft name and refreshes updatedAt timestamp")
        void renameOwn() {
            // given
            sut.createEmpty(ALICE_ID, null, "Old");
            DraftRow row = listDraftsByOwnerOrdered().getFirst();
            Instant before = row.getUpdatedAt();
            // when
            sut.rename(row.getDraftId(), "NewName", ALICE_ID);
            // then
            DraftRow after = getDraft(row.getDraftId()).orElseThrow();
            assertEquals("NewName", after.getName());
            assertTrue(after.getUpdatedAt().compareTo(before) >= 0);
        }

        @Test
        @DisplayName("delete removes draft only if owner matches")
        void deleteOwnOnly() {
            // given
            sut.createEmpty(ALICE_ID, null, "Mine");
            DraftRow row = listDraftsByOwnerOrdered().getFirst();
            // when & then
            sut.delete(row.getDraftId(), "11111111-2222-3333-4444-555555555555");
            assertTrue(getDraft(row.getDraftId()).isPresent(), "Foreign user should not delete");
            sut.delete(row.getDraftId(), ALICE_ID);
            assertTrue(getDraft(row.getDraftId()).isEmpty(), "Owner can delete their own draft");
        }
    }
}