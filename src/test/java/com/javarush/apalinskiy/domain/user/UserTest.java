package com.javarush.apalinskiy.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User")
class UserTest {

    @Nested
    @DisplayName("of()")
    class OfFactory {

        @Test
        @DisplayName("sets fields when created then trimmed/lowercased where needed")
        void setsFields() {
            // Given
            String name = "  John  ";
            String login = "  AdminUser  ";
            String pass = "secret";
            // When
            User u = User.of(Role.ADMIN, name, login, pass);
            // Then
            assertEquals(Role.ADMIN, u.getRole());
            assertEquals("John", u.getUserName());
            assertEquals("adminuser", u.getUserLogin());
            assertEquals("secret", u.getPassword());
            assertNotNull(u.getUserId());
        }

        @Test
        @DisplayName("defaults role to USER when null then role=USER")
        void defaultsRoleToUserWhenNull() {
            // Given
            // When
            User u = User.of(null, "Name", "login", "p");
            // Then
            assertEquals(Role.USER, u.getRole());
        }

        @Test
        @DisplayName("generates UUID userId when created then matches pattern")
        void generatesUuid() {
            // Given / When
            User u = User.of(Role.USER, "n", "l", "p");
            // Then
            assertTrue(u.getUserId().matches("^[0-9a-f\\-]+$"));
        }

        @Test
        @DisplayName("createdAt is ~now when created then within bounds")
        void createdAtIsNow() {
            // Given
            Instant before = Instant.now();
            // When
            User u = User.of(Role.USER, "n", "l", "p");
            Instant after = Instant.now();
            // Then
            assertFalse(u.getCreatedAt().isBefore(before));
            assertFalse(u.getCreatedAt().isAfter(after));
        }

        @Test
        @DisplayName("throws when name or login or password blank then IAE")
        void throwsOnBlankRequired() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class, () -> User.of(Role.USER, " ", "l", "p"));
            assertThrows(IllegalArgumentException.class, () -> User.of(Role.USER, "n", " ", "p"));
            assertThrows(IllegalArgumentException.class, () -> User.of(Role.USER, "n", "l", " "));
        }
    }

    @Nested
    @DisplayName("withId() / withUserName() / withPassword() / withRole() / withLogin()")
    class Withers {

        @Test
        @DisplayName("withId replaces id when called then new id")
        void withIdReplacesId() {
            // Given
            User u = User.of(Role.USER, "n", "l", "p");
            // When
            User u2 = u.withId("custom-id");
            // Then
            assertEquals("custom-id", u2.getUserId());
            assertNotSame(u, u2);
        }

        @Test
        @DisplayName("withUserName trims name when set then trimmed")
        void withUserNameTrims() {
            // Given
            User u = User.of(Role.USER, "n", "l", "p");
            // When
            User u2 = u.withUserName("  New Name  ");
            // Then
            assertEquals("New Name", u2.getUserName());
        }

        @Test
        @DisplayName("withPassword replaces password when set then new password")
        void withPasswordReplaces() {
            // Given
            User u = User.of(Role.USER, "n", "l", "p");
            // When
            User u2 = u.withPassword("newP");
            // Then
            assertEquals("newP", u2.getPassword());
        }

        @Test
        @DisplayName("withRole changes role when non-null then updated")
        void withRoleChanges() {
            // Given
            User u = User.of(Role.USER, "n", "l", "p");
            // When
            User u2 = u.withRole(Role.ADMIN);
            // Then
            assertEquals(Role.ADMIN, u2.getRole());
        }

        @Test
        @DisplayName("withRole keeps old role when null passed then unchanged")
        void withRoleNullKeepsOld() {
            // Given
            User u = User.of(Role.ADMIN, "n", "l", "p");
            // When
            User u2 = u.withRole(null);
            // Then
            assertEquals(Role.ADMIN, u2.getRole());
            assertNotSame(u, u2); // новый инстанс
        }

        @Test
        @DisplayName("withLogin lowercases/trim when set then normalized")
        void withLoginNormalizes() {
            // Given
            User u = User.of(Role.USER, "n", "l", "p");
            // When
            User u2 = u.withLogin("  NewLOGIN  ");
            // Then
            assertEquals("newlogin", u2.getUserLogin());
        }

        @Test
        @DisplayName("withers preserve createdAt when used then same instant")
        void withersPreserveCreatedAt() {
            // Given
            User u = User.of(Role.USER, "n", "l", "p");
            Instant created = u.getCreatedAt();
            // When
            User u2 = u.withUserName("x");
            // Then
            assertEquals(created, u2.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("getCreatedAtDate()")
    class CreatedAtDate {

        @Test
        @DisplayName("converts Instant to Date when called then equals Date.from(instant)")
        void convertsToDate() {
            // Given
            User u = User.of(Role.USER, "n", "l", "p");
            // When
            Date d = u.getCreatedAtDate();
            // Then
            assertEquals(Date.from(u.getCreatedAt()), d);
        }
    }

    @Nested
    @DisplayName("equals/hashCode")
    class Equality {

        @Test
        @DisplayName("equals by userId when same id then equal")
        void equalsById() {
            // Given
            User u1 = User.of(Role.USER, "Name", "login", "p");
            User u2 = u1.withUserName("Other");
            // When
            boolean eq = u1.equals(u2);
            // Then
            assertTrue(eq);
            assertEquals(u1.hashCode(), u2.hashCode());
        }

        @Test
        @DisplayName("not equal when different ids then false")
        void notEqualDifferentIds() {
            // Given
            User u1 = User.of(Role.USER, "Name", "login", "p");
            User u2 = u1.withId("another-id");
            // When / Then
            assertNotEquals(u1, u2);
        }

        @Test
        @DisplayName("equals contracts: reflexive, null, different type")
        void equalsContracts() {
            // Given
            User u = User.of(Role.USER, "n", "l", "p");
            // Then
            //noinspection EqualsWithItself
            assertEquals(u, u);
            assertNotEquals(null, u);
            //noinspection AssertBetweenInconvertibleTypes
            assertNotEquals("string", u);
        }
    }
}