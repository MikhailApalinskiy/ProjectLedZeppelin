package com.javarush.apalinskiy.domain.social;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FriendRequest")
class FriendRequestTest {

    @Nested
    @DisplayName("of()")
    class OfFactory {

        @Test
        @DisplayName("sets fields when created then from/to id kept")
        void setsFields() {
            // Given
            String from = "u1";
            String to = "u2";
            // When
            FriendRequest fr = FriendRequest.of(from, to);
            // Then
            assertEquals(from, fr.getFromUserId());
            assertEquals(to, fr.getToUserId());
        }

        @Test
        @DisplayName("generates UUID id when created then matches pattern")
        void generatesUuid() {
            // Given / When
            FriendRequest fr = FriendRequest.of("a", "b");
            // Then
            assertTrue(fr.getId().matches("^[0-9a-f\\-]+$"));
        }

        @Test
        @DisplayName("ids are unique when created twice then different")
        void idsAreUnique() {
            // Given / When
            FriendRequest a = FriendRequest.of("x", "y");
            FriendRequest b = FriendRequest.of("x", "y");
            // Then
            assertNotEquals(a.getId(), b.getId());
        }

        @Test
        @DisplayName("createdAt is ~now when created then within bounds")
        void createdAtIsNow() {
            // Given
            Instant before = Instant.now();
            // When
            FriendRequest fr = FriendRequest.of("a", "b");
            Instant after = Instant.now();
            // Then
            assertFalse(fr.getCreatedAt().isBefore(before));
            assertFalse(fr.getCreatedAt().isAfter(after));
        }
    }

    @Nested
    @DisplayName("getCreatedAtDate()")
    class GetCreatedAtDate {

        @Test
        @DisplayName("converts Instant to Date when called then equals Date.from(instant)")
        void convertsToDate() {
            // Given
            FriendRequest fr = FriendRequest.of("a", "b");
            // When
            Date d = fr.getCreatedAtDate();
            // Then
            assertEquals(Date.from(fr.getCreatedAt()), d);
        }
    }

    @Nested
    @DisplayName("private ctor branch")
    class PrivateCtorBranch {

        @Test
        @DisplayName("sets createdAt when passed null then not null")
        void setsCreatedAtWhenNull() throws Exception {
            // Given
            Constructor<FriendRequest> ctor =
                    FriendRequest.class.getDeclaredConstructor(String.class, String.class, String.class, Instant.class);
            ctor.setAccessible(true);
            // When
            FriendRequest fr = ctor.newInstance("id1", "from", "to", null);
            // Then
            assertNotNull(fr.getCreatedAt());
        }
    }
}