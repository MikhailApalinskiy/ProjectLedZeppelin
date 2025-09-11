package com.javarush.apalinskiy.domain.notify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Notification")
class NotificationTest {

    @Nested
    @DisplayName("of()")
    class OfFactory {

        @Test
        @DisplayName("creates with given fields when FRIEND_REQUEST then ok")
        void createsWithGivenFields() {
            // Given
            String userId = "u1";
            // When
            Notification n = Notification.of(userId, NotificationType.FRIEND_REQUEST, "title", "body");
            // Then
            assertEquals(userId, n.getUserId());
            assertEquals(NotificationType.FRIEND_REQUEST, n.getType());
            assertEquals("title", n.getTitle());
            assertEquals("body", n.getBody());
            assertNotNull(n.getId());
            assertFalse(n.isRead());
        }

        @Test
        @DisplayName("generates unique UUID id when created twice then different")
        void idIsUuid() {
            // Given
            // When
            Notification n1 = Notification.of("u1", NotificationType.FRIEND_REQUEST, "t", "b");
            Notification n2 = Notification.of("u1", NotificationType.FRIEND_REQUEST, "t", "b");
            // Then
            assertNotEquals(n1.getId(), n2.getId());
            assertTrue(n1.getId().matches("^[0-9a-f\\-]+$"));
        }

        @Test
        @DisplayName("sets createdAt ~ now when created then within bounds")
        void createdAtIsNow() {
            // Given
            Instant before = Instant.now();
            // When
            Notification n = Notification.of("u1", NotificationType.FRIEND_REQUEST, "t", "b");
            Instant after = Instant.now();
            // Then
            assertFalse(n.getCreatedAt().isBefore(before));
            assertFalse(n.getCreatedAt().isAfter(after));
        }
    }

    @Nested
    @DisplayName("markRead()")
    class MarkRead {

        @Test
        @DisplayName("returns new instance with read=true when called then original remains false")
        void setsReadTrue() {
            // Given
            Notification n = Notification.of("u1", NotificationType.FRIEND_REQUEST, "t", "b");
            // When
            Notification marked = n.markRead();
            // Then
            assertTrue(marked.isRead());
            assertFalse(n.isRead());
        }

        @Test
        @DisplayName("preserves id when marked then equal")
        void preservesId() {
            // Given
            Notification n = Notification.of("u1", NotificationType.FRIEND_REQUEST, "t", "b");
            String id = n.getId();
            // When
            Notification marked = n.markRead();
            // Then
            assertEquals(id, marked.getId());
        }

        @Test
        @DisplayName("preserves createdAt when marked then equal")
        void preservesCreatedAt() {
            // Given
            Notification n = Notification.of("u1", NotificationType.FRIEND_REQUEST, "t", "b");
            Instant created = n.getCreatedAt();
            // When
            Notification marked = n.markRead();
            // Then
            assertEquals(created, marked.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("getCreatedAtDate()")
    class GetCreatedAtDate {

        @Test
        @DisplayName("converts Instant to Date when requested then equals Date.from(instant)")
        void convertsInstantToDate() {
            // Given
            Notification n = Notification.of("u1", NotificationType.FRIEND_REQUEST, "t", "b");
            // When
            Date d = n.getCreatedAtDate();
            // Then
            assertEquals(Date.from(n.getCreatedAt()), d);
        }
    }

    @Nested
    @DisplayName("NotificationType enum")
    class NotificationTypeEnum {

        @Test
        @DisplayName("has expected size 7 when values() called then 7")
        void hasSevenValues() {
            // Given
            // When
            int len = NotificationType.values().length;
            // Then
            assertEquals(7, len);
        }

        @Test
        @DisplayName("contains FRIEND_REQUEST when valueOf then enum constant")
        void containsFriendRequest() {
            // Given
            String name = "FRIEND_REQUEST";
            // When
            NotificationType t = NotificationType.valueOf(name);
            // Then
            assertEquals(NotificationType.FRIEND_REQUEST, t);
        }
    }
}