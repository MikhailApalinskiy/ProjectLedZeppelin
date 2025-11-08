package com.javarush.apalinskiy.repository.hibernate.notify;

import com.javarush.apalinskiy.domain.notify.Notification;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.utils.HibernateUtil;
import it.AbstractDbTest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HNotificationRepository (integration, seeded data)")
class HNotificationRepositoryIT extends AbstractDbTest {

    private HNotificationRepository sut;
    private SessionFactory sf;
    private static MockedStatic<HibernateUtil> UTIL;

    private static final String ADMIN_ID = "11111111-2222-3333-4444-555555555555";
    private static final String ALICE_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String SEEDED_NOTIF_ID = "00000000-0000-0000-0000-000000000101";

    @BeforeAll
    static void initStatics() {
        UTIL = Mockito.mockStatic(HibernateUtil.class);
    }

    @AfterAll
    static void closeStatics() {
        if (UTIL != null) UTIL.close();
    }

    @BeforeEach
    void setUpRepo() {
        this.sf = super.sessionFactory;
        UTIL.when(HibernateUtil::getSessionFactory).thenReturn(sf);
        this.sut = new HNotificationRepository();
        assertNotNull(this.sut);
    }

    private User findUser(String id) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            User u = s.get(User.class, id);
            s.getTransaction().commit();
            return u;
        }
    }

    private Notification findNotif(String id) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            Notification n = s.get(Notification.class, id);
            s.getTransaction().commit();
            return n;
        }
    }

    private List<Notification> listByUser(String userId) {
        try (Session s = sf.openSession()) {
            s.beginTransaction();
            List<Notification> list = s.createQuery(
                            "from Notification n where n.user.userId = :uid order by n.createdAt desc",
                            Notification.class)
                    .setParameter("uid", userId)
                    .list();
            s.getTransaction().commit();
            return list;
        }
    }

    @Nested
    @DisplayName("using seeded data from Liquibase (context: test)")
    class SeededData {

        @BeforeEach
        void resetSeededState() {
            setUnread();
        }

        private void setUnread() {
            try (Session s = sf.openSession()) {
                s.beginTransaction();
                s.createMutationQuery("update Notification n set n.read=false where n.id=:id")
                        .setParameter("id", HNotificationRepositoryIT.SEEDED_NOTIF_ID)
                        .executeUpdate();
                s.getTransaction().commit();
            }
        }

        @Test
        @DisplayName("finds seeded Admin and his welcome notification")
        void findsSeededData() {
            // Given
            // When
            User admin = findUser(ADMIN_ID);
            Notification n = findNotif(SEEDED_NOTIF_ID);
            // Then
            assertNotNull(admin);
            assertEquals("admin", admin.getUserLogin());
            assertNotNull(n);
            assertEquals("Добро пожаловать!", n.getTitle());
            assertEquals(ADMIN_ID, n.getUser().getUserId());
            assertFalse(n.getRead()); // теперь стабильно false
        }

        @Test
        @DisplayName("markRead() updates seeded Admin notification to read")
        void markReadSeeded() {
            // Given
            Notification before = findNotif(SEEDED_NOTIF_ID);
            assertFalse(before.getRead(), "Should be unread initially");
            // When
            sut.markRead(ADMIN_ID, SEEDED_NOTIF_ID);
            // Then
            Notification after = findNotif(SEEDED_NOTIF_ID);
            assertTrue(after.getRead(), "Should become read");
        }

        @Test
        @DisplayName("unreadCount() reflects seeded Admin notification before and after markRead()")
        void unreadCountWithSeeded() {
            // Given
            // When
            int before = sut.unreadCount(ADMIN_ID);
            // Then
            assertTrue(before >= 1, "Should have at least one unread notification");
            // When
            sut.markRead(ADMIN_ID, SEEDED_NOTIF_ID);
            // Then
            int after = sut.unreadCount(ADMIN_ID);
            assertEquals(before - 1, after);
        }
    }

    @Nested
    @DisplayName("when adding new notifications")
    class SaveAndList {

        @Test
        @DisplayName("save() merges and persists new notification for existing Alice")
        void saveNewForAlice() {
            // Given
            User alice = findUser(ALICE_ID);
            Notification n = Notification.of(alice, NotificationType.FRIEND_REQUEST,
                    "Test", "body");
            String nid = n.getId();
            // When
            try (Session s = sf.getCurrentSession()) {
                s.beginTransaction();
                sut.save(n);
                s.getTransaction().commit();
            }
            // Then
            Notification got = findNotif(nid);
            assertNotNull(got);
            assertEquals("Test", got.getTitle());
            assertEquals(ALICE_ID, got.getUser().getUserId());
        }

        @Test
        @DisplayName("list() returns notifications ordered desc by createdAt for seeded user")
        void listSeededUser() {
            // Given
            // When
            List<Notification> list = sut.list(ALICE_ID, 10, 0);
            // Then
            assertFalse(list.isEmpty());
            for (int i = 1; i < list.size(); i++) {
                assertFalse(list.get(i).getCreatedAt().isAfter(list.get(i - 1).getCreatedAt()), "Notifications should be ordered descending");
            }
        }
    }

    @Nested
    @DisplayName("markAllRead() and clearAll()")
    class BulkOps {

        @Test
        @DisplayName("markAllRead() marks all of Alice’s unread notifications")
        void markAllSeeded() {
            // Given
            // When
            sut.markAllRead(ALICE_ID);
            // Then
            List<Notification> list = listByUser(ALICE_ID);
            assertTrue(list.stream().allMatch(n -> n.getRead() != null && n.getRead()));
        }

        @Test
        @DisplayName("clearAll() removes all notifications for Alice but not for Admin")
        void clearAllRespectsUser() {
            // Given
            int beforeAdmin = listByUser(ADMIN_ID).size();
            // When
            sut.clearAll(ALICE_ID);
            // Then
            int afterAlice = listByUser(ALICE_ID).size();
            int afterAdmin = listByUser(ADMIN_ID).size();
            assertEquals(0, afterAlice, "Alice’s notifications should be deleted");
            assertEquals(beforeAdmin, afterAdmin, "Admin’s notifications must remain untouched");
        }
    }
}