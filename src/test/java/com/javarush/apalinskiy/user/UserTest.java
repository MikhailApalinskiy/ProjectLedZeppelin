package com.javarush.apalinskiy.user;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User u;

    @BeforeEach
    void setUp() {
        u = User.of(Role.USER, " User ", " USER ", "user");
    }

    @Nested
    class Of {

        @Test
        void normalizesFieldsNameTrimLoginLowerPasswordAsIsTest() {
            // given — setUp
            // when / then
            assertAll(
                    () -> assertEquals("User", u.getUserName()),
                    () -> assertEquals("user", u.getUserLogin()),
                    () -> assertEquals("user", u.getPassword())
            );
        }

        @Test
        void roleNullDefaultsToUserTest() {
            // given / when
            User usr = User.of(null, "User", "user", "user");
            // then
            assertEquals(Role.USER, usr.getRole());
        }

        @Test
        void createdAtWithinBoundsTest() {
            // given
            Instant t0 = Instant.now();
            // when
            User usr = User.of(Role.USER, "User", "user", "user");
            Instant t1 = Instant.now();
            // then
            assertFalse(usr.getCreatedAt().isBefore(t0));
            assertFalse(usr.getCreatedAt().isAfter(t1));
        }

        @Test
        void userIdIsUuidTest() {
            // given — setUp
            // when / then
            assertDoesNotThrow(() -> UUID.fromString(u.getUserId()));
        }

        @Test
        void userIdIsNotBlankTest() {
            // given — setUp
            // when / then
            assertFalse(u.getUserId().isBlank());
        }

        @Test
        void userIdIsNotNullTest() {
            // given — setUp
            // when / then
            assertNotNull(u.getUserId());
        }

        @Test
        void generatedIdsAreUniqueAcrossInstancesTest() {
            // given
            User another = User.of(Role.USER, "User", "user", "user");
            // when / then
            assertNotEquals(u.getUserId(), another.getUserId());
        }

        @Nested
        class Validation {

            @Test
            void throwsOnNullNameWithMessageTest() {
                // given / when
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> User.of(Role.USER, null, "user", "user"));
                // then
                assertEquals("Username or login or password are required", ex.getMessage());
            }

            @Test
            void throwsOnNullLoginWithMessageTest() {
                // given / when
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> User.of(Role.USER, "user", null, "user"));
                // then
                assertEquals("Username or login or password are required", ex.getMessage());
            }

            @Test
            void throwsOnNullPasswordWithMessageTest() {
                // given / when
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> User.of(Role.USER, "user", "user", null));
                // then
                assertEquals("Username or login or password are required", ex.getMessage());
            }

            @ParameterizedTest
            @ValueSource(strings = {"", " ", "\t", "\n", "\r\n"})
            void throwsOnBlankNameWithMessageTest(String input) {
                // given / when
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> User.of(Role.USER, input, "user", "user"));
                // then
                assertEquals("Username or login or password are required", ex.getMessage());
            }

            @ParameterizedTest
            @ValueSource(strings = {"", " ", "\t", "\n", "\r\n"})
            void throwsOnBlankLoginWithMessageTest(String input) {
                // given / when
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> User.of(Role.USER, "User", input, "user"));
                // then
                assertEquals("Username or login or password are required", ex.getMessage());
            }

            @ParameterizedTest
            @ValueSource(strings = {"", " ", "\t", "\n", "\r\n"})
            void throwsOnBlankPasswordWithMessageTest(String input) {
                // given / when
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> User.of(Role.USER, "User", "user", input));
                // then
                assertEquals("Username or login or password are required", ex.getMessage());
            }
        }
    }

    @Nested
    class WithId {

        @Test
        void returnsNewInstanceAndCopiesAllButIdTest() {
            // given
            String newId = UUID.randomUUID().toString();
            // when
            User u2 = u.withId(newId);
            // then
            assertNotSame(u, u2);
            assertEquals(u.getUserLogin(), u2.getUserLogin());
            assertEquals(u.getUserName(), u2.getUserName());
            assertEquals(u.getPassword(), u2.getPassword());
            assertEquals(u.getRole(), u2.getRole());
            assertNotEquals(u.getUserId(), u2.getUserId());
        }

        @Test
        void setsExactIdAndKeepsOtherFieldsIncludingCreatedAtTest() {
            // given
            String exact = "abc";
            // when
            User u2 = u.withId(exact);
            // then
            assertEquals(exact, u2.getUserId());
            assertEquals(u.getCreatedAt(), u2.getCreatedAt());
            assertEquals(u.getUserLogin(), u2.getUserLogin());
            assertEquals(u.getUserName(), u2.getUserName());
            assertEquals(u.getPassword(), u2.getPassword());
            assertEquals(u.getRole(), u2.getRole());
        }

        @Test
        void nullIdCausesHashCodeToThrowNpeTest() {
            // given
            User usr = u.withId(null);
            // when / then
            assertThrows(NullPointerException.class, usr::hashCode);
        }
    }

    @Nested
    class EqualsHashCode {

        @Test
        void reflexiveTest() {
            // given / when / then
            assertTrue(u.equals(u));
        }

        @Test
        void symmetricForSameIdAndHashCodeEqualOnDifferentInstancesTest() {
            // given
            User u1 = User.of(Role.USER, "A", "a", "p");
            User u2 = User.of(Role.USER, "B", "b", "q").withId(u1.getUserId());
            // when / then
            assertNotSame(u1, u2);
            assertTrue(u1.equals(u2));
            assertTrue(u2.equals(u1));
            assertEquals(u1.hashCode(), u2.hashCode());
        }

        @Test
        void transitiveForSameIdOnDifferentInstancesTest() {
            // given
            User u1 = User.of(Role.USER, "A", "a", "p");
            String id = u1.getUserId();
            User u2 = User.of(Role.USER, "B", "b", "q").withId(id);
            User u3 = User.of(Role.USER, "C", "c", "r").withId(id);
            // when / then
            assertTrue(u1.equals(u2));
            assertTrue(u2.equals(u3));
            assertTrue(u1.equals(u3));
        }

        @Test
        void hashCodeConsistentForSameInstanceTest() {
            // given / when / then
            assertEquals(u.hashCode(), u.hashCode());
        }

        @Test
        void hashCodeDiffersForDifferentIdsTest() {
            // given
            User another = u.withId(UUID.randomUUID().toString());
            // when / then
            assertNotEquals(u.getUserId(), another.getUserId());
            assertNotEquals(u.hashCode(), another.hashCode());
        }

        @Test
        void equalsNullReturnsFalseTest() {
            // given / when / then
            assertFalse(u.equals(null));
        }

        @Test
        void equalsOtherTypeReturnsFalseTest() {
            // given / when / then
            assertFalse(u.equals(new Object()));
        }

        @Test
        void equalsOtherHasNullIdReturnsFalseNoThrowTest() {
            // given
            User other = u.withId(null);
            // when / then
            assertDoesNotThrow(() -> assertFalse(u.equals(other)));
        }

        @Test
        void equalsThisHasNullIdThrowsNpeTest() {
            // given
            User withNull = u.withId(null);
            // when / then
            assertThrows(NullPointerException.class, () -> withNull.equals(u));
        }
    }

    @Nested
    class I18n {

        @Test
        void loginLowercasedWithLocaleRootTurkishCaseTest() {
            // given / when
            User usr = User.of(Role.USER, "User", "İI", "pwd");
            // then
            assertEquals("i̇i", usr.getUserLogin());
        }
    }
}