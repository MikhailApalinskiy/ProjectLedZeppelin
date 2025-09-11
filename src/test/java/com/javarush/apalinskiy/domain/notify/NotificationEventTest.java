package com.javarush.apalinskiy.domain.notify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NotificationEvent")
class NotificationEventTest {

    @Nested
    @DisplayName("of()")
    class FactoryOf {

        @Test
        @DisplayName("creates with given fields when FRIEND_REQUEST then ok")
        void createsWithGivenFields() {
            // Given
            Map<String, String> data = Map.of("k", "v");
            // When
            NotificationEvent ev = NotificationEvent.of(
                    NotificationType.FRIEND_REQUEST, "actor1", "target1", data
            );
            // Then
            assertEquals(NotificationType.FRIEND_REQUEST, ev.type());
            assertEquals("actor1", ev.actorUserId());
            assertEquals("target1", ev.targetUserId());
            assertSame(data, ev.data());
        }

        @Test
        @DisplayName("sets occurredAt ~ now when created then within bounds")
        void occurredAtIsNow() {
            // Given
            Instant before = Instant.now();
            // When
            NotificationEvent ev = NotificationEvent.of(
                    NotificationType.FRIEND_REQUEST, "a", "t", Map.of()
            );
            Instant after = Instant.now();
            // Then
            assertFalse(ev.occurredAt().isBefore(before));
            assertFalse(ev.occurredAt().isAfter(after));
        }
    }

    @Nested
    @DisplayName("canonical constructor")
    class CanonicalCtor {

        @Test
        @DisplayName("preserves provided occurredAt when explicit then equal")
        void preservesOccurredAt() {
            // Given
            Instant ts = Instant.parse("2024-01-02T03:04:05Z");
            // When
            NotificationEvent ev = new NotificationEvent(
                    NotificationType.USER_ADMIN_CHANGED, "admin", "user", Map.of(), ts
            );
            // Then
            assertEquals(ts, ev.occurredAt());
        }
    }

    @Nested
    @DisplayName("record semantics")
    class RecordSemantics {

        @Test
        @DisplayName("equals/hashCode when same components then equal")
        void equalsAndHashCode() {
            // Given
            Instant ts = Instant.parse("2024-05-06T07:08:09Z");
            Map<String, String> data = Map.of("x", "1");
            // When
            NotificationEvent a = new NotificationEvent(NotificationType.QUEST_MODERATED, "m1", "u1", data, ts);
            NotificationEvent b = new NotificationEvent(NotificationType.QUEST_MODERATED, "m1", "u1", data, ts);
            // Then
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("toString contains type and users when called then readable")
        void toStringContainsKeyFields() {
            // Given
            NotificationEvent ev = new NotificationEvent(
                    NotificationType.FRIEND_REMOVED, "a1", "t1", Map.of("reason", "block"), Instant.EPOCH
            );
            // When
            String s = ev.toString();
            // Then
            assertTrue(s.contains("FRIEND_REMOVED"));
            assertTrue(s.contains("a1"));
            assertTrue(s.contains("t1"));
        }
    }
}