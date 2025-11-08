package com.javarush.apalinskiy.service.impl.user;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.user.UserStats;
import com.javarush.apalinskiy.utils.HibernateUtil;
import it.AbstractDbTest;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HUserStatsService (IT)")
class HUserStatsServiceIT extends AbstractDbTest {

    private HUserStatsService sut;
    private SessionFactory sf;

    private static MockedStatic<HibernateUtil> UTIL;

    private static final String ADMIN_ID = "11111111-2222-3333-4444-555555555555";
    private static final String ALICE_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    private UserStats stats(String uid) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            UserStats st = s.createQuery("""
                    from UserStats us
                    where us.user.userId = :uid
                    """, UserStats.class)
                    .setParameter("uid", uid)
                    .setMaxResults(1)
                    .uniqueResult();
            if (st != null) {
                Hibernate.initialize(st.getMainFinals());
            }
            s.getTransaction().commit();
            return st;
        }
    }

    private void deleteStats(String uid) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            s.createMutationQuery("delete from UserStats us where us.user.userId = :uid")
                    .setParameter("uid", uid)
                    .executeUpdate();
            s.getTransaction().commit();
        }
    }

    private void createUser(String id) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            User u = new User();
            u.setUserId(id);
            u.setUserLogin("tempuser");
            u.setUserName("Temp User");
            u.setPassword("test");
            u.setRole(Role.USER);
            u.setCreatedAt(Instant.now());
            s.persist(u);
            s.getTransaction().commit();
        }
    }

    @BeforeAll
    static void staticInit() {
    }

    @AfterAll
    static void staticClose() {
        if (UTIL != null) {
            UTIL.close();
        }
    }

    @BeforeEach
    void initService() {
        this.sf = super.sessionFactory;
        UTIL = Mockito.mockStatic(HibernateUtil.class);
        UTIL.when(HibernateUtil::getSessionFactory).thenReturn(this.sf);
        this.sut = new HUserStatsService();
        assertNotNull(sut);
    }

    @AfterEach
    void tearDownService() {
        if (UTIL != null) {
            UTIL.close();
            UTIL = null;
        }
    }

    @Nested
    @DisplayName("incCreated(userId)")
    class IncCreated {

        @Test
        @DisplayName("Given Alice has no stats — When incCreated — Then auto-creates stats and sets created=1")
        void alice_NoStats_thenCreatesAndIncrements() {
            // Given
            deleteStats(ALICE_ID);
            // When
            sut.incCreated(ALICE_ID);
            // Then
            UserStats st = stats(ALICE_ID);
            assertNotNull(st, "Stats must be created");
            assertEquals(1L, st.getQuestsCreated());
            assertEquals(0L, st.getQuestsCompleted());
            assertEquals(0L, st.getEndingsUnlocked());
        }

        @Test
        @DisplayName("Given Admin seeded created=1 — When incCreated — Then becomes 2")
        void admin_Seeded_thenBecomes2() {
            // Given
            // When
            sut.incCreated(ADMIN_ID);
            // Then
            UserStats st = stats(ADMIN_ID);
            assertNotNull(st);
            assertEquals(2L, st.getQuestsCreated(), "Seeded 1 + 1 call = 2");
        }
    }

    @Nested
    @DisplayName("onQuestCompleted(userId, questKey, finalNodeId)")
    class OnQuestCompleted {

        @Test
        @DisplayName("Given no stats — When complete non-main — Then completed=1, endings=0, finals empty")
        void nonMain_IncrementsCompletedOnly() {
            // Given
            deleteStats(ALICE_ID);
            // When
            sut.onQuestCompleted(ALICE_ID, "side", 777);
            // Then
            UserStats st = stats(ALICE_ID);
            assertNotNull(st);
            assertEquals(1L, st.getQuestsCompleted());
            assertEquals(0L, st.getEndingsUnlocked());
            assertTrue(st.getMainFinals().isEmpty());
        }

        @Test
        @DisplayName("Given no stats — When complete main with new final — Then endingsUnlocked++ and final stored")
        void main_NewEnding_IncrementsEndingsAndStoresFinal() {
            // Given
            deleteStats(ALICE_ID);
            // When
            sut.onQuestCompleted(ALICE_ID, "main", 42);
            // Then
            UserStats st = stats(ALICE_ID);
            assertNotNull(st);
            assertEquals(1L, st.getQuestsCompleted());
            assertEquals(1L, st.getEndingsUnlocked());
            assertTrue(st.getMainFinals().contains(42));
        }

        @Test
        @DisplayName("Given final already unlocked — When repeat main with same final — Then endings not incremented")
        void main_SameEnding_NoDoubleCount() {
            // Given
            deleteStats(ALICE_ID);
            sut.onQuestCompleted(ALICE_ID, "main", 7);
            // When
            sut.onQuestCompleted(ALICE_ID, "main", 7);
            // Then
            UserStats st = stats(ALICE_ID);
            assertNotNull(st);
            assertEquals(2L, st.getQuestsCompleted(), "Completed increments every time");
            assertEquals(1L, st.getEndingsUnlocked(), "But endingsUnlocked stays the same");
            assertTrue(st.getMainFinals().contains(7));
        }
    }

    @Nested
    @DisplayName("statsOf(userId)")
    class StatsOf {

        @Test
        @DisplayName("Given Admin has persisted stats — When statsOf — Then returns persisted values (created>=1)")
        void admin_SeededStats_ReturnsPersisted() {
            // Given
            // When
            UserStats st = sut.statsOf(ADMIN_ID);
            // Then
            assertNotNull(st);
            assertTrue(st.getQuestsCreated() >= 1L, "Seeded created must be at least 1");
        }

        @Test
        @DisplayName("Given new user without stats — When statsOf — Then returns zero-filled placeholder")
        void newUser_NoStats_ReturnsZeroFilled() {
            // Given
            String uid = "00000000-1111-2222-3333-444444444444";
            createUser(uid);
            deleteStats(uid);
            // When
            UserStats st = sut.statsOf(uid);
            // Then
            assertNotNull(st);
            assertEquals(0L, st.getQuestsCreated());
            assertEquals(0L, st.getQuestsCompleted());
            assertEquals(0L, st.getEndingsUnlocked());
            assertTrue(st.getMainFinals().isEmpty());
        }
    }
}