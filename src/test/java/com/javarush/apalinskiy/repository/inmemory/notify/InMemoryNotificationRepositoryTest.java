package com.javarush.apalinskiy.repository.inmemory.notify;

import com.javarush.apalinskiy.domain.notify.Notification;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryNotificationRepository")
class InMemoryNotificationRepositoryTest {

    private InMemoryNotificationRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryNotificationRepository();
    }

    private Notification n(String id, String user, Instant t, boolean read) throws Exception {
        // Given
        Constructor<Notification> c = Notification.class.getDeclaredConstructor(
                String.class, String.class, NotificationType.class,
                String.class, String.class, Instant.class, boolean.class
        );
        c.setAccessible(true);
        // When
        return c.newInstance(id, user, NotificationType.FRIEND_REQUEST, "title", "body", t, read);
    }

    @Nested
    @DisplayName("save() + list()")
    class SaveAndList {

        @Test
        @DisplayName("saves by user when listed then only that user is returned")
        void savesByUser() throws Exception {
            // Given
            repo.save(n("a1", "u1", Instant.now(), false));
            repo.save(n("a2", "u2", Instant.now(), false));
            // When
            List<Notification> list = repo.list("u1", 10, 0);
            // Then
            assertEquals(1, list.size());
            assertEquals("u1", list.getFirst().getUserId());
        }

        @Test
        @DisplayName("sorts by createdAt desc when listed then latest first")
        void sortsByCreatedAtDesc() throws Exception {
            // Given
            Instant t1 = Instant.parse("2024-01-01T00:00:00Z");
            Instant t2 = Instant.parse("2024-01-02T00:00:00Z");
            Instant t3 = Instant.parse("2024-01-03T00:00:00Z");
            repo.save(n("n1", "u1", t1, false));
            repo.save(n("n2", "u1", t3, false));
            repo.save(n("n3", "u1", t2, false));
            // When
            List<Notification> list = repo.list("u1", 10, 0);
            // Then
            assertEquals(List.of(t3, t2, t1), list.stream().map(Notification::getCreatedAt).toList());
        }

        @Test
        @DisplayName("applies limit/offset when listed then window returned")
        void appliesLimitOffset() throws Exception {
            // Given
            Instant t1 = Instant.parse("2024-01-01T00:00:00Z");
            Instant t2 = Instant.parse("2024-01-02T00:00:00Z");
            Instant t3 = Instant.parse("2024-01-03T00:00:00Z");
            repo.save(n("n1", "u", t1, false));
            repo.save(n("n2", "u", t2, false));
            repo.save(n("n3", "u", t3, false));
            // When
            List<Notification> page = repo.list("u", 2, 1);
            // Then
            assertEquals(List.of(t2, t1), page.stream().map(Notification::getCreatedAt).toList());
        }

        @Test
        @DisplayName("limit<=0 defaults to 50 when listed then returns all small sets")
        void limitDefaultsTo50() throws Exception {
            // Given
            repo.save(n("x1", "u", Instant.now(), false));
            repo.save(n("x2", "u", Instant.now(), false));
            // When
            List<Notification> all = repo.list("u", 0, 0);
            // Then
            assertEquals(2, all.size());
        }

        @Test
        @DisplayName("saving same id overwrites entry when saved then single item remains")
        void overwritesSameId() throws Exception {
            // Given
            Instant t1 = Instant.parse("2024-01-01T00:00:00Z");
            Instant t2 = Instant.parse("2024-01-02T00:00:00Z");
            repo.save(n("same", "u", t1, false));
            // When
            repo.save(n("same", "u", t2, true));
            // Then
            List<Notification> list = repo.list("u", 10, 0);
            assertEquals(1, list.size());
            assertEquals(t2, list.getFirst().getCreatedAt());
            assertTrue(list.getFirst().isRead());
        }
    }

    @Nested
    @DisplayName("unreadCount()")
    class UnreadCount {

        @Test
        @DisplayName("counts only unread when mixed then correct")
        void countsOnlyUnread() throws Exception {
            // Given
            repo.save(n("a", "u", Instant.now(), false));
            repo.save(n("b", "u", Instant.now(), true));
            repo.save(n("c", "u", Instant.now(), false));
            // When
            int cnt = repo.unreadCount("u");
            // Then
            assertEquals(2, cnt);
        }

        @Test
        @DisplayName("returns zero when no notifications then 0")
        void returnsZeroWhenNone() {
            // Given / When
            int cnt = repo.unreadCount("nouser");
            // Then
            assertEquals(0, cnt);
        }
    }

    @Nested
    @DisplayName("markAllRead()")
    class MarkAllRead {

        @Test
        @DisplayName("marks all as read when called then unreadCount=0")
        void marksAll() throws Exception {
            // Given
            repo.save(n("a", "u", Instant.now(), false));
            repo.save(n("b", "u", Instant.now(), false));
            // When
            repo.markAllRead("u");
            // Then
            assertEquals(0, repo.unreadCount("u"));
        }

        @Test
        @DisplayName("no-op for unknown user when called then no throw")
        void noopUnknownUser() {
            // Given / When / Then
            assertDoesNotThrow(() -> repo.markAllRead("nouser"));
        }
    }

    @Nested
    @DisplayName("clearAll()")
    class ClearAll {

        @Test
        @DisplayName("removes all for user when called then list empty")
        void removesAll() throws Exception {
            // Given
            repo.save(n("a", "u", Instant.now(), false));
            // When
            repo.clearAll("u");
            // Then
            assertTrue(repo.list("u", 10, 0).isEmpty());
            assertEquals(0, repo.unreadCount("u"));
        }
    }

    @Nested
    @DisplayName("markRead()")
    class MarkReadSingle {

        @Test
        @DisplayName("keeps already-read as is and marks unread when called then both branches executed")
        void keepsReadAndMarksUnread() throws Exception {
            // Given
            Instant older = Instant.parse("2024-01-01T00:00:00Z");
            Instant newer = Instant.parse("2024-01-02T00:00:00Z");
            Notification unread = n("u1", "u", older, false);
            Notification already = n("r1", "u", newer, true);
            repo.save(unread);
            repo.save(already);
            // When
            repo.markAllRead("u");
            // Then
            Notification unreadAfter = repo.list("u", 10, 0).stream()
                    .filter(x -> x.getId().equals("u1")).findFirst().orElseThrow();
            assertTrue(unreadAfter.isRead());
            assertNotSame(unread, unreadAfter);
            Notification alreadyAfter = repo.list("u", 10, 0).stream()
                    .filter(x -> x.getId().equals("r1")).findFirst().orElseThrow();
            assertTrue(alreadyAfter.isRead());
            assertSame(already, alreadyAfter);
            assertEquals(2, repo.list("u", 10, 0).size());
            assertEquals(0, repo.unreadCount("u"));
        }

        @Test
        @DisplayName("marks one as read when unread then unreadCount decremented")
        void marksOne() throws Exception {
            // Given
            repo.save(n("a", "u", Instant.now(), false));
            repo.save(n("b", "u", Instant.now(), false));
            // When
            repo.markRead("u", "a");
            // Then
            assertEquals(1, repo.unreadCount("u"));
        }

        @Test
        @DisplayName("does nothing when already read then unchanged")
        void doesNothingIfAlreadyRead() throws Exception {
            // Given
            repo.save(n("a", "u", Instant.now(), true));
            // When
            repo.markRead("u", "a");
            // Then
            assertEquals(0, repo.unreadCount("u"));
        }

        @Test
        @DisplayName("no-op when id not found then unchanged")
        void noOpOnMissingId() throws Exception {
            // Given
            repo.save(n("a", "u", Instant.now(), false));
            // When
            repo.markRead("u", "zzz");
            // Then
            assertEquals(1, repo.unreadCount("u"));
        }

        @Test
        @DisplayName("no-op for unknown user then no throw")
        void noOpForUnknownUser() {
            // Given / When / Then
            assertDoesNotThrow(() -> repo.markRead("nouser", "id"));
        }
    }
}