package com.javarush.apalinskiy.service.impl.save;

import com.javarush.apalinskiy.domain.save.SaveState;
import com.javarush.apalinskiy.repository.hibernate.save.HSaveStateRepository;
import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HSaveStateService — unit tests with construction mocking (Given / When / Then)")
class HSaveStateServiceTest {

    @Mock
    SessionFactory sessionFactory;
    @Mock
    Session session;
    @Mock
    Transaction tx;

    private MockedStatic<HibernateUtil> mockedHibernateUtil;

    @BeforeEach
    void init() {
        mockedHibernateUtil = mockStatic(HibernateUtil.class);
        mockedHibernateUtil.when(HibernateUtil::getSessionFactory).thenReturn(sessionFactory);
        when(sessionFactory.getCurrentSession()).thenReturn(session);
        when(session.getTransaction()).thenReturn(tx);
    }

    @AfterEach
    void cleanup() {
        mockedHibernateUtil.close();
    }

    @Nested
    @DisplayName("getOrCreate(userId)")
    class GetOrCreate {

        @Test
        @DisplayName("starts local tx, commits, restores RO, delegates to repo")
        void startsTxAndCommits() {
            // Given
            String uid = "user-1";
            SaveState st = new SaveState();
            st.setUserId(uid);
            st.setSlotCount(10);
            when(tx.isActive()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            when(session.isDefaultReadOnly()).thenReturn(true);
            try (MockedConstruction<HSaveStateRepository> mocked =
                         mockConstruction(HSaveStateRepository.class,
                                 (repoMock, ctx) -> when(repoMock.getOrCreate(uid, 10)).thenReturn(st))) {
                HSaveStateService sut = new HSaveStateService();
                // When
                SaveState result = sut.getOrCreate(uid);
                // Then
                verify(session).beginTransaction();
                verify(session).setDefaultReadOnly(false);
                HSaveStateRepository repo = mocked.constructed().getFirst();
                verify(repo).getOrCreate(uid, 10);
                verify(tx).commit();
                verify(session).setDefaultReadOnly(true);
                assertSame(st, result);
            }
        }

        @Test
        @DisplayName("does not start tx when already active")
        void alreadyActive_noTxStart() {
            // Given
            when(tx.isActive()).thenReturn(true);
            SaveState st = new SaveState();
            st.setUserId("u");
            st.setSlotCount(10);
            try (MockedConstruction<HSaveStateRepository> mocked =
                         mockConstruction(HSaveStateRepository.class,
                                 (repoMock, ctx) -> when(repoMock.getOrCreate("u", 10)).thenReturn(st))) {
                HSaveStateService sut = new HSaveStateService();
                // When
                SaveState out = sut.getOrCreate("u");
                // Then
                verify(session, never()).beginTransaction();
                verify(tx, never()).commit();
                verify(session, never()).setDefaultReadOnly(anyBoolean());
                HSaveStateRepository repo = mocked.constructed().getFirst();
                verify(repo).getOrCreate("u", 10);
                assertSame(st, out);
            }
        }

        @Test
        @DisplayName("rolls back and restores RO on failure")
        void repoThrows_rollback() {
            // Given
            when(tx.isActive()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            when(session.isDefaultReadOnly()).thenReturn(true);
            try (MockedConstruction<HSaveStateRepository> ignored =
                         mockConstruction(HSaveStateRepository.class,
                                 (repoMock, ctx) ->
                                         when(repoMock.getOrCreate(anyString(), anyInt()))
                                                 .thenThrow(new RuntimeException("boom")))) {
                HSaveStateService sut = new HSaveStateService();
                // When / Then
                assertThrows(RuntimeException.class, () -> sut.getOrCreate("x"));
                verify(tx).rollback();
                verify(session).setDefaultReadOnly(true);
            }
        }
    }

    @Nested
    @DisplayName("getGlobalSlot(userId, slot)")
    class GetGlobalSlot {

        @Test
        @DisplayName("starts local tx, sets RO=true, commits and restores")
        void startsTx_readonly_true() {
            // Given
            String uid = "u";
            int slot = 0;
            SaveStateService.GlobalSlot dto = new SaveStateService.GlobalSlot(
                    0, 1, "Start", "q-1", "Quest", Instant.now());
            when(tx.isActive()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            when(session.isDefaultReadOnly()).thenReturn(false);
            try (MockedConstruction<HSaveStateRepository> mocked =
                         mockConstruction(HSaveStateRepository.class,
                                 (repoMock, ctx) ->
                                         when(repoMock.getGlobalSlot(uid, slot)).thenReturn(Optional.of(dto)))) {
                HSaveStateService sut = new HSaveStateService();
                // When
                Optional<SaveStateService.GlobalSlot> out = sut.getGlobalSlot(uid, slot);
                // Then
                verify(session).beginTransaction();
                verify(session).setDefaultReadOnly(true);
                HSaveStateRepository repo = mocked.constructed().getFirst();
                verify(repo).getGlobalSlot(uid, slot);
                verify(tx).commit();
                verify(session).setDefaultReadOnly(true);
                assertTrue(out.isPresent());
                assertEquals("q-1", out.get().questId());
            }
        }

        @Test
        @DisplayName("already active tx — no local begin/commit")
        void alreadyActive_noLocalTx() {
            // Given
            when(tx.isActive()).thenReturn(true);
            try (MockedConstruction<HSaveStateRepository> mocked =
                         mockConstruction(HSaveStateRepository.class,
                                 (repoMock, ctx) ->
                                         when(repoMock.getGlobalSlot("u", 5)).thenReturn(Optional.empty()))) {
                HSaveStateService sut = new HSaveStateService();
                // When
                Optional<SaveStateService.GlobalSlot> out = sut.getGlobalSlot("u", 5);
                // Then
                verify(session, never()).beginTransaction();
                verify(tx, never()).commit();
                verify(session, never()).setDefaultReadOnly(anyBoolean());
                HSaveStateRepository repo = mocked.constructed().getFirst();
                verify(repo).getGlobalSlot("u", 5);
                assertTrue(out.isEmpty());
            }
        }

        @Test
        @DisplayName("rolls back on exception")
        void repoFailure_rollsBack() {
            // Given
            when(tx.isActive()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            when(session.isDefaultReadOnly()).thenReturn(false);
            try (MockedConstruction<HSaveStateRepository> ignored =
                         mockConstruction(HSaveStateRepository.class,
                                 (repoMock, ctx) ->
                                         when(repoMock.getGlobalSlot(anyString(), anyInt()))
                                                 .thenThrow(new RuntimeException("fail")))) {
                HSaveStateService sut = new HSaveStateService();
                // When / Then
                assertThrows(RuntimeException.class, () -> sut.getGlobalSlot("u", 1));
                verify(tx).rollback();
                verify(session).setDefaultReadOnly(true);
            }
        }
    }

    @Nested
    @DisplayName("setGlobalSlot(userId, slot, questId, questName, nodeId, title)")
    class SetGlobalSlot {

        @Test
        @DisplayName("starts local tx, commits, restores RO")
        void startsTx_commits() {
            // Given
            when(tx.isActive()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            when(session.isDefaultReadOnly()).thenReturn(true);
            try (MockedConstruction<HSaveStateRepository> mocked =
                         mockConstruction(HSaveStateRepository.class)) {
                HSaveStateService sut = new HSaveStateService();
                // When
                sut.setGlobalSlot("u", 1, "q", "Quest", 42, "Checkpoint");
                // Then
                verify(session).beginTransaction();
                verify(session).setDefaultReadOnly(false);
                HSaveStateRepository repo = mocked.constructed().getFirst();
                verify(repo).setGlobalSlot("u", 1, "q", "Quest", 42, "Checkpoint");
                verify(tx).commit();
                verify(session).setDefaultReadOnly(true);
            }
        }

        @Test
        @DisplayName("already active tx — no local begin/commit")
        void alreadyActive_noLocal() {
            // Given
            when(tx.isActive()).thenReturn(true);
            try (MockedConstruction<HSaveStateRepository> mocked =
                         mockConstruction(HSaveStateRepository.class)) {
                HSaveStateService sut = new HSaveStateService();
                // When
                sut.setGlobalSlot("u", 1, "q", "Quest", 42, "Checkpoint");
                // Then
                verify(session, never()).beginTransaction();
                verify(tx, never()).commit();
                verify(session, never()).setDefaultReadOnly(anyBoolean());
                HSaveStateRepository repo = mocked.constructed().getFirst();
                verify(repo).setGlobalSlot("u", 1, "q", "Quest", 42, "Checkpoint");
            }
        }

        @Test
        @DisplayName("rolls back on repo exception")
        void repoThrows_rollback() {
            // Given
            when(tx.isActive()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            when(session.isDefaultReadOnly()).thenReturn(true);
            try (MockedConstruction<HSaveStateRepository> ignored =
                         mockConstruction(HSaveStateRepository.class,
                                 (repoMock, ctx) ->
                                         doThrow(new RuntimeException("boom"))
                                                 .when(repoMock)
                                                 .setGlobalSlot(anyString(), anyInt(), any(), any(), anyInt(), anyString()))) {
                HSaveStateService sut = new HSaveStateService();
                // When / Then
                assertThrows(RuntimeException.class,
                        () -> sut.setGlobalSlot("u", 1, "q", "Quest", 42, "Checkpoint"));
                verify(tx).rollback();
                verify(session).setDefaultReadOnly(true);
            }
        }
    }

    @Nested
    @DisplayName("clearGlobalSlot(userId, slot)")
    class ClearGlobalSlot {

        @Test
        @DisplayName("starts local tx, commits, restores RO")
        void startsTx_commits() {
            // Given
            when(tx.isActive()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            when(session.isDefaultReadOnly()).thenReturn(true);

            try (MockedConstruction<HSaveStateRepository> mocked =
                         mockConstruction(HSaveStateRepository.class)) {
                HSaveStateService sut = new HSaveStateService();
                // When
                sut.clearGlobalSlot("u", 3);
                // Then
                verify(session).beginTransaction();
                verify(session).setDefaultReadOnly(false);
                HSaveStateRepository repo = mocked.constructed().getFirst();
                verify(repo).clearGlobalSlot("u", 3);
                verify(tx).commit();
                verify(session).setDefaultReadOnly(true);
            }
        }

        @Test
        @DisplayName("rolls back on failure")
        void repoThrows_rollback() {
            // Given
            when(tx.isActive()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            when(session.isDefaultReadOnly()).thenReturn(true);
            try (MockedConstruction<HSaveStateRepository> ignored =
                         mockConstruction(HSaveStateRepository.class,
                                 (repoMock, ctx) ->
                                         doThrow(new RuntimeException("fail"))
                                                 .when(repoMock).clearGlobalSlot(anyString(), anyInt()))) {
                HSaveStateService sut = new HSaveStateService();
                // When / Then
                assertThrows(RuntimeException.class, () -> sut.clearGlobalSlot("u", 3));
                verify(tx).rollback();
                verify(session).setDefaultReadOnly(true);
            }
        }
    }
}