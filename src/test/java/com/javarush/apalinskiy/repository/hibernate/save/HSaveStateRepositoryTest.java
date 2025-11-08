package com.javarush.apalinskiy.repository.hibernate.save;

import com.javarush.apalinskiy.domain.save.GlobalSlot;
import com.javarush.apalinskiy.domain.save.GlobalSlotId;
import com.javarush.apalinskiy.domain.save.SaveState;
import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.utils.HibernateUtil;
import it.AbstractDbTest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@DisplayName("HSaveStateRepository (IT) — seeded by Liquibase testdata")
class HSaveStateRepositoryIT extends AbstractDbTest {

    private static final String ADMIN_ID = "11111111-2222-3333-4444-555555555555";
    private static final String ALICE_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String SEED_QUEST_ID = "99999999-1111-2222-3333-444444444444";

    private SessionFactory sf;
    private Transaction tx;
    private HSaveStateRepository sut;

    private MockedStatic<HibernateUtil> mockedHibernateUtil;

    @BeforeEach
    void setSessionFactory() {
        this.sf = super.sessionFactory;
        Session s = sf.getCurrentSession();
        tx = s.beginTransaction();
        mockedHibernateUtil = mockStatic(HibernateUtil.class);
        mockedHibernateUtil.when(HibernateUtil::getSessionFactory).thenReturn(sf);
        sut = new HSaveStateRepository();
    }

    @AfterEach
    void tearDown() {
        if (tx != null && tx.isActive()) {
            tx.rollback();
        }
        if (mockedHibernateUtil != null) {
            mockedHibernateUtil.close();
        }
    }

    private SaveState getState(String userId) {
        return sf.getCurrentSession().get(SaveState.class, userId);
    }

    private GlobalSlot getGlobalSlotEntity(String userId, int slot) {
        return sf.getCurrentSession().get(GlobalSlot.class, new GlobalSlotId(userId, slot));
    }

    @Nested
    @DisplayName("getOrCreate(userId, defaultSlotCount)")
    class GetOrCreate {

        @Test
        @DisplayName("returns existing SaveState for seeded Admin (slot_count=10)")
        void returnsExisting() {
            // when
            SaveState st = sut.getOrCreate(ADMIN_ID, 99);
            // then
            assertNotNull(st);
            assertEquals(ADMIN_ID, st.getUserId());
            assertEquals(10, st.getSlotCount(), "Should keep seeded slot_count");
        }

        @Test
        @DisplayName("creates new SaveState for Alice when missing, with provided default slots (>=1)")
        void createsNewForAlice() {
            // given
            assertNull(getState(ALICE_ID), "Alice has no SaveState in seeds");
            // when
            SaveState st = sut.getOrCreate(ALICE_ID, 7);
            // then
            assertNotNull(st);
            assertEquals(ALICE_ID, st.getUserId());
            assertEquals(7, st.getSlotCount());
            assertNotNull(st.getUpdatedAt());
            assertNotNull(getState(ALICE_ID), "Entity persisted");
        }

        @Test
        @DisplayName("throws if user does not exist")
        void throwsIfUserMissing() {
            // given
            String ghost = "00000000-0000-0000-0000-000000000000";
            // when / then
            assertThrows(IllegalArgumentException.class, () -> sut.getOrCreate(ghost, 5));
        }
    }

    @Nested
    @DisplayName("getGlobalSlot(userId, slot)")
    class GetGlobalSlot {

        @Test
        @DisplayName("returns seeded slot #0 for Admin with correct DTO values")
        void returnsSeeded() {
            // when
            Optional<SaveStateService.GlobalSlot> dtoOpt = sut.getGlobalSlot(ADMIN_ID, 0);
            // then
            assertTrue(dtoOpt.isPresent());
            SaveStateService.GlobalSlot dto = dtoOpt.get();
            assertEquals(0, dto.index());
            assertEquals(1, dto.nodeId());
            assertEquals("Старт", dto.title());
            assertEquals(SEED_QUEST_ID, dto.questId());
            assertEquals("Demo Quest", dto.questName());
            assertNotNull(dto.updatedAt());
        }

        @Test
        @DisplayName("returns empty for non-existing slot")
        void emptyForMissingSlot() {
            // when
            Optional<SaveStateService.GlobalSlot> dtoOpt = sut.getGlobalSlot(ADMIN_ID, 9);
            // then
            assertTrue(dtoOpt.isEmpty());
        }
    }

    @Nested
    @DisplayName("setGlobalSlot(userId, slot, questId, questName, nodeId, title)")
    class SetGlobalSlot {

        @Test
        @DisplayName("updates existing Admin slot #0 (nodeId/title changed) and bumps SaveState.updatedAt")
        void updatesExistingSlot() {
            // given
            SaveState before = getState(ADMIN_ID);
            assertNotNull(before);
            Instant prevUpdated = before.getUpdatedAt();
            // when
            sut.setGlobalSlot(ADMIN_ID, 0, SEED_QUEST_ID, "Demo Quest", 2, "Продвинулся");
            // then
            GlobalSlot gs = getGlobalSlotEntity(ADMIN_ID, 0);
            assertNotNull(gs);
            assertEquals(2, gs.getNodeId());
            assertEquals("Продвинулся", gs.getTitle());
            assertEquals(SEED_QUEST_ID, gs.getQuestId());
            assertEquals("Demo Quest", gs.getQuestName());
            SaveState after = getState(ADMIN_ID);
            assertNotNull(after.getUpdatedAt());
            assertTrue(after.getUpdatedAt().compareTo(prevUpdated) >= 0, "SaveState.updatedAt should be refreshed");
        }

        @Test
        @DisplayName("creates SaveState for Alice on-the-fly and writes slot #3")
        void createsStateAndSlotForAlice() {
            // given
            assertNull(getState(ALICE_ID));
            // when
            sut.setGlobalSlot(ALICE_ID, 3, SEED_QUEST_ID, "Demo Quest", 1, "Начало");
            // then
            SaveState st = getState(ALICE_ID);
            assertNotNull(st, "SaveState should be created automatically");
            assertEquals(ALICE_ID, st.getUserId());
            GlobalSlot gs = getGlobalSlotEntity(ALICE_ID, 3);
            assertNotNull(gs);
            assertEquals(1, gs.getNodeId());
            assertEquals("Начало", gs.getTitle());
            assertEquals(SEED_QUEST_ID, gs.getQuestId());
            assertEquals("Demo Quest", gs.getQuestName());
        }
    }

    @Nested
    @DisplayName("clearGlobalSlot(userId, slot)")
    class ClearGlobalSlot {

        @Test
        @DisplayName("removes existing Admin slot #0 and refreshes SaveState.updatedAt")
        void clearsExisting() throws InterruptedException {
            // given
            SaveState before = getState(ADMIN_ID);
            assertNotNull(before);
            Instant prev = before.getUpdatedAt();
            Thread.sleep(5);
            // when
            sut.clearGlobalSlot(ADMIN_ID, 0);
            // then
            assertNull(getGlobalSlotEntity(ADMIN_ID, 0), "Slot entity should be removed");
            SaveState after = getState(ADMIN_ID);
            assertNotNull(after);
            assertTrue(after.getUpdatedAt().compareTo(prev) >= 0, "SaveState.updatedAt should be refreshed");
        }

        @Test
        @DisplayName("no-op when slot doesn’t exist (no exception)")
        void noOpWhenMissing() {
            // when / then
            assertDoesNotThrow(() -> sut.clearGlobalSlot(ADMIN_ID, 999));
        }
    }
}