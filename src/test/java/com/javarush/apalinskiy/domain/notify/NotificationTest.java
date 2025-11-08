package com.javarush.apalinskiy.domain.notify;

import com.javarush.apalinskiy.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Notification entity")
class NotificationTest {

    @Nested
    @DisplayName("factory method 'of'")
    class OfFactoryMethod {

        @Test
        @DisplayName("should generate non-null id")
        void generatesNonNullId() {
            // Given
            User user = new User();
            // When
            Notification n = Notification.of(user, NotificationType.FRIEND_REQUEST, "t", null);
            // Then
            assertNotNull(n.getId());
        }

        @Test
        @DisplayName("should generate a valid UUID string")
        void generatesValidUuid() {
            // Given
            User user = new User();
            // When
            Notification n = Notification.of(user, NotificationType.FRIEND_REQUEST, "t", null);
            // Then
            assertDoesNotThrow(() -> UUID.fromString(n.getId()));
        }

        @Test
        @DisplayName("should assign provided user")
        void assignsUser() {
            // Given
            User user = new User();
            // When
            Notification n = Notification.of(user, NotificationType.FRIEND_REQUEST, "t", null);
            // Then
            assertSame(user, n.getUser());
        }

        @Test
        @DisplayName("should assign provided type")
        void assignsType() {
            // Given
            User user = new User();
            NotificationType type = NotificationType.USER_ADMIN_CHANGED;
            // When
            Notification n = Notification.of(user, type, "t", null);
            // Then
            assertEquals(type, n.getType());
        }

        @Test
        @DisplayName("should assign provided title")
        void assignsTitle() {
            // Given
            User user = new User();
            String title = "Welcome!";
            // When
            Notification n = Notification.of(user, NotificationType.FRIEND_REQUEST, title, null);
            // Then
            assertEquals(title, n.getTitle());
        }

        @Test
        @DisplayName("should assign provided body (nullable)")
        void assignsBody() {
            // Given
            User user = new User();
            String body = "Hello there";
            // When
            Notification n = Notification.of(user, NotificationType.FRIEND_REQUEST, "t", body);
            // Then
            assertEquals(body, n.getBody());
        }

        @Test
        @DisplayName("should set createdAt to non-null")
        void setsCreatedAtNonNull() {
            // Given
            User user = new User();
            // When
            Notification n = Notification.of(user, NotificationType.FRIEND_REQUEST, "t", null);
            // Then
            assertNotNull(n.getCreatedAt());
        }

        @Test
        @DisplayName("should set read=false by default")
        void setsUnreadByDefault() {
            // Given
            User user = new User();
            // When
            Notification n = Notification.of(user, NotificationType.FRIEND_REQUEST, "t", null);
            // Then
            assertFalse(n.getRead());
        }

        @Test
        @DisplayName("should set createdAt approximately to now")
        void setsCreatedAtApproximatelyNow() {
            // Given
            Instant before = Instant.now();
            User user = new User();
            // When
            Notification n = Notification.of(user, NotificationType.FRIEND_REQUEST, "t", null);
            Instant after = Instant.now();
            // Then
            assertFalse(n.getCreatedAt().isBefore(before), "createdAt must be >= before");
            assertFalse(n.getCreatedAt().isAfter(after), "createdAt must be <= after");
        }
    }

    @Nested
    @DisplayName("method 'getCreatedAtDate'")
    class GetCreatedAtDate {

        @Test
        @DisplayName("should convert createdAt Instant to Date")
        void convertsInstantToDate() {
            // Given
            Notification n = new Notification();
            Instant ts = Instant.now();
            n.setCreatedAt(ts);
            // When
            Date d = n.getCreatedAtDate();
            // Then
            assertEquals(Date.from(ts), d);
        }
    }

    @Nested
    @DisplayName("accessors (getters/setters)")
    class Accessors {

        @Test
        @DisplayName("should set/get id")
        void idAccessor() {
            // Given
            Notification n = new Notification();
            String id = UUID.randomUUID().toString();
            // When
            n.setId(id);
            // Then
            assertEquals(id, n.getId());
        }

        @Test
        @DisplayName("should set/get user")
        void userAccessor() {
            // Given
            Notification n = new Notification();
            User u = new User();
            // When
            n.setUser(u);
            // Then
            assertSame(u, n.getUser());
        }

        @Test
        @DisplayName("should set/get type")
        void typeAccessor() {
            // Given
            Notification n = new Notification();
            // When
            n.setType(NotificationType.FRIEND_REQUEST);
            // Then
            assertEquals(NotificationType.FRIEND_REQUEST, n.getType());
        }

        @Test
        @DisplayName("should set/get title")
        void titleAccessor() {
            // Given
            Notification n = new Notification();
            // When
            n.setTitle("X");
            // Then
            assertEquals("X", n.getTitle());
        }

        @Test
        @DisplayName("should set/get body")
        void bodyAccessor() {
            // Given
            Notification n = new Notification();
            // When
            n.setBody("Y");
            // Then
            assertEquals("Y", n.getBody());
        }

        @Test
        @DisplayName("should set/get createdAt")
        void createdAtAccessor() {
            // Given
            Notification n = new Notification();
            Instant ts = Instant.now();
            // When
            n.setCreatedAt(ts);
            // Then
            assertEquals(ts, n.getCreatedAt());
        }

        @Test
        @DisplayName("should set/get read flag")
        void readAccessor() {
            // Given
            Notification n = new Notification();
            // When
            n.setRead(true);
            // Then
            assertTrue(n.getRead());
        }
    }
}