package com.javarush.apalinskiy.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User")
class UserTest {

    @Nested
    @DisplayName("Factory method: of")
    class FactoryMethod {

        @Test
        @DisplayName("creates user with generated UUID and defaults")
        void createsUserWithDefaults() {
            // Given
            Role role = Role.USER;
            String name = "Alice";
            String login = "AliceLogin";
            String pass = "secret6";
            // When
            User u = User.of(role, name, login, pass);
            // Then
            assertNotNull(u.getUserId(), "id must be generated");
            assertEquals(Role.USER, u.getRole());
            assertEquals("Alice", u.getUserName());
            assertEquals("alicelogin", u.getUserLogin(), "login must be lowercased");
            assertEquals("secret6", u.getPassword());
            assertNotNull(u.getCreatedAt(), "createdAt must be set");
        }

        @Test
        @DisplayName("defaults role to USER when null")
        void defaultsRoleWhenNull() {
            // Given
            String name = "Bob";
            String login = "BobLogin";
            String pass = "qwerty6";
            // When
            User u = User.of(null, name, login, pass);
            // Then
            assertEquals(Role.USER, u.getRole());
        }

        @Test
        @DisplayName("throws when username/login/password are blank")
        void throwsOnBlankFields() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class, () -> User.of(Role.USER, " ", "login", "123456"));
            assertThrows(IllegalArgumentException.class, () -> User.of(Role.USER, "Name", " ", "123456"));
            assertThrows(IllegalArgumentException.class, () -> User.of(Role.USER, "Name", "login", " "));
        }

        @Test
        @DisplayName("throws when password is shorter than 6 chars")
        void throwsOnShortPassword() {
            // Given
            String shortPwd = "12345";
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> User.of(Role.USER, "Name", "login", shortPwd));
        }

        @Test
        @DisplayName("throws when username or login exceed 50 chars")
        void throwsOnTooLongNameOrLogin() {
            // Given
            String long51 = "x".repeat(51);
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> User.of(Role.USER, long51, "login", "123456"));
            assertThrows(IllegalArgumentException.class, () -> User.of(Role.USER, "Name", long51, "123456"));
        }
    }

    @Nested
    @DisplayName("Setters normalization & validation")
    class Setters {

        @Test
        @DisplayName("setUserLogin trims and lowercases login")
        void setUserLoginNormalizes() {
            // Given
            User u = User.of(Role.USER, "Name", "login", "123456");
            String input = "  MiXeD_Login  ";
            // When
            u.setUserLogin(input);
            // Then
            assertEquals("mixed_login", u.getUserLogin());
        }

        @Test
        @DisplayName("setUserLogin throws when > 50 chars")
        void setUserLoginTooLong() {
            // Given
            User u = User.of(Role.USER, "Name", "login", "123456");
            String long51 = "x".repeat(51);
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> u.setUserLogin(long51));
        }

        @Test
        @DisplayName("setUserName throws when > 50 chars")
        void setUserNameTooLong() {
            // Given
            User u = User.of(Role.USER, "Name", "login", "123456");
            String long51 = "x".repeat(51);
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> u.setUserName(long51));
        }
    }

    @Nested
    @DisplayName("withId copy")
    class WithIdCopy {

        @Test
        @DisplayName("returns a new user with same fields and new id")
        void returnsCopyWithNewId() {
            // Given
            User original = User.of(Role.ADMIN, "Alice", "alice", "123456");
            String newId = UUID.randomUUID().toString();
            // When
            User copy = original.withId(newId);
            // Then
            assertNotSame(original, copy);
            assertEquals(newId, copy.getUserId());
            assertEquals(original.getRole(), copy.getRole());
            assertEquals(original.getUserName(), copy.getUserName());
            assertEquals(original.getUserLogin(), copy.getUserLogin());
            assertEquals(original.getPassword(), copy.getPassword());
            assertEquals(original.getCreatedAt(), copy.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Equality & hashCode")
    class Equality {

        @Test
        @DisplayName("equals and hashCode depend only on userId")
        void equalsHashOnIdOnly() {
            // Given
            String id = UUID.randomUUID().toString();
            User a = User.of(Role.USER, "N1", "l1", "123456").withId(id);
            User b = User.of(Role.ADMIN, "N2", "l2", "abcdef").withId(id);
            // When
            boolean eq = a.equals(b);
            // Then
            assertTrue(eq);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("not equal when ids differ")
        void notEqualOnDifferentIds() {
            // Given
            User a = User.of(Role.USER, "N1", "l1", "123456").withId(UUID.randomUUID().toString());
            User b = User.of(Role.USER, "N1", "l1", "123456").withId(UUID.randomUUID().toString());
            // When
            boolean eq = a.equals(b);
            // Then
            assertFalse(eq);
        }

        @SuppressWarnings("AssertBetweenInconvertibleTypes")
        @Test
        @DisplayName("equals is reflexive and type-safe")
        void equalsReflexiveAndTypeSafe() {
            // Given
            User a = User.of(Role.USER, "N", "l", "123456");
            // When / Then
            assertEquals(a, a);
            assertNotEquals("not a user", a);
        }
    }

    @Nested
    @DisplayName("createdAt & date conversion")
    class CreatedAtAndDate {

        @Test
        @DisplayName("createdAt defaults to now")
        void createdAtDefaultsToNow() {
            // Given / When
            User u = User.of(Role.USER, "N", "l", "123456");
            // Then
            assertNotNull(u.getCreatedAt());
        }

        @Test
        @DisplayName("getCreatedAtDate returns Date.from(createdAt)")
        void getCreatedAtDateConverts() {
            // Given
            User u = User.of(Role.USER, "N", "l", "123456");
            Instant created = u.getCreatedAt();
            // When
            Date d = u.getCreatedAtDate();
            // Then
            assertEquals(Date.from(created), d);
        }
    }

    @Nested
    @DisplayName("Role handling")
    class RoleHandling {

        @Test
        @DisplayName("explicit role is preserved")
        void explicitRolePreserved() {
            // Given / When
            User u = User.of(Role.ADMIN, "N", "l", "123456");
            // Then
            assertEquals(Role.ADMIN, u.getRole());
        }

        @Test
        @DisplayName("null role defaults to USER")
        void nullRoleBecomesUser() {
            // Given / When
            User u = User.of(null, "N", "l", "123456");
            // Then
            assertEquals(Role.USER, u.getRole());
        }
    }
}