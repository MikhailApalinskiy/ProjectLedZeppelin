package com.javarush.apalinskiy.domain.social;

import com.javarush.apalinskiy.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FriendRequest")
class FriendRequestTest {

    @Nested
    @DisplayName("Factory method: of()")
    class FactoryMethodOf {

        @Test
        @DisplayName("creates PENDING request with from/to users set and createdAt initialized")
        void createsPendingWithUsersAndTimestamp() {
            // Given
            User from = new User();
            from.setUserId("from-uid");
            User to = new User();
            to.setUserId("to-uid");
            // When
            FriendRequest fr = FriendRequest.of(from, to);
            // Then
            assertSame(from, fr.getFromUser(), "from user must be set");
            assertSame(to, fr.getToUser(), "to user must be set");
            assertEquals(FriendRequest.Status.PENDING, fr.getStatus(), "status must be PENDING");
            assertNotNull(fr.getCreatedAt(), "createdAt must be initialized");
            assertNull(fr.getRespondedAt(), "respondedAt should not be set by factory");
            assertNull(fr.getId(), "id should not be set by factory (left for @PrePersist)");
        }
    }

    @Nested
    @DisplayName("Lifecycle: @PrePersist")
    class LifecyclePrePersist {

        @Test
        @DisplayName("generates UUID when id is null/blank")
        void generatesUuidIfIdMissing() {
            // Given
            FriendRequest fr = new FriendRequest();
            fr.setFromUser(new User());
            fr.setToUser(new User());
            // When
            fr.prePersist();
            // Then
            assertNotNull(fr.getId(), "id must be generated");
            assertFalse(fr.getId().isBlank(), "generated id must be non-blank");
        }

        @Test
        @DisplayName("keeps existing id if already set")
        void keepsExistingId() {
            // Given
            FriendRequest fr = new FriendRequest();
            fr.setFromUser(new User());
            fr.setToUser(new User());
            fr.setId("fixed-id");
            // When
            fr.prePersist();
            // Then
            assertEquals("fixed-id", fr.getId(), "existing id must be preserved");
        }

        @Test
        @DisplayName("initializes createdAt when null")
        void initializesCreatedAtIfNull() {
            // Given
            FriendRequest fr = new FriendRequest();
            fr.setFromUser(new User());
            fr.setToUser(new User());
            fr.setCreatedAt(null);
            // When
            fr.prePersist();
            // Then
            assertNotNull(fr.getCreatedAt(), "createdAt must be initialized");
        }

        @Test
        @DisplayName("keeps existing createdAt if present")
        void keepsExistingCreatedAt() {
            // Given
            Instant ts = Instant.parse("2025-01-01T00:00:00Z");
            FriendRequest fr = new FriendRequest();
            fr.setFromUser(new User());
            fr.setToUser(new User());
            fr.setCreatedAt(ts);
            // When
            fr.prePersist();
            // Then
            assertSame(ts, fr.getCreatedAt(), "createdAt must not be overwritten");
        }

        @Test
        @DisplayName("defaults status to PENDING if null")
        void defaultsStatusToPendingIfNull() {
            // Given
            FriendRequest fr = new FriendRequest();
            fr.setFromUser(new User());
            fr.setToUser(new User());
            fr.setStatus(null);
            // When
            fr.prePersist();
            // Then
            assertEquals(FriendRequest.Status.PENDING, fr.getStatus(), "status must default to PENDING");
        }

        @Test
        @DisplayName("keeps existing non-null status")
        void keepsExistingStatus() {
            // Given
            FriendRequest fr = new FriendRequest();
            fr.setFromUser(new User());
            fr.setToUser(new User());
            fr.setStatus(FriendRequest.Status.ACCEPTED);
            // When
            fr.prePersist();
            // Then
            assertEquals(FriendRequest.Status.ACCEPTED, fr.getStatus(), "existing status must be preserved");
        }
    }

    @Nested
    @DisplayName("Accessors and helpers")
    class Accessors {

        @Test
        @DisplayName("getCreatedAtDate converts Instant to Date")
        void getCreatedAtDateConvertsInstant() {
            // Given
            Instant now = Instant.parse("2025-02-02T10:15:30Z");
            FriendRequest fr = new FriendRequest();
            fr.setCreatedAt(now);
            // When
            Date d = fr.getCreatedAtDate();
            // Then
            assertNotNull(d, "Date must be returned");
            assertEquals(now.toEpochMilli(), d.getTime(), "Date time must match Instant epoch millis");
        }
    }
}