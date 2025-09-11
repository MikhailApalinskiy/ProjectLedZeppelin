package com.javarush.apalinskiy.service.impl.user;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultUserService")
class DefaultUserServiceTest {

    @Mock
    UserRepository repo;

    DefaultUserService sut;

    private User u(String id, String name, String login, String pass) {
        return User.of(Role.USER, name, login, pass).withId(id);
    }

    @BeforeEach
    void setUp() {
        sut = new DefaultUserService(repo);
    }

    @Nested
    @DisplayName("register(role, name, login, rawPassword)")
    class Register {

        @Test
        @DisplayName("saves immediately when id unique then returns user")
        void savesImmediately() {
            // Given / When
            User user = sut.register(Role.USER, "Alice", "alice", "p@ssw0rd");
            // Then
            assertEquals("alice", user.getUserLogin());
            verify(repo, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("retries on DuplicateIdException then succeeds")
        void retriesOnDuplicateIdThenSuccess() {
            // Given
            doThrow(new DuplicateIdException("dup1"))
                    .doNothing()
                    .when(repo).save(any(User.class));
            // When
            User user = sut.register(Role.USER, "Bob", "bob", "secret!");
            // Then
            assertEquals("bob", user.getUserLogin());
            verify(repo, times(2)).save(any(User.class));
        }

        @Test
        @DisplayName("fails after 3 DuplicateIdException then ISE")
        void failsAfterThreeRetries() {
            // Given
            doThrow(new DuplicateIdException("1"))
                    .doThrow(new DuplicateIdException("2"))
                    .doThrow(new DuplicateIdException("3"))
                    .when(repo).save(any(User.class));
            // When / Then
            assertThrows(IllegalStateException.class,
                    () -> sut.register(Role.USER, "C", "c", "passwd1"));
            verify(repo, times(3)).save(any(User.class));
        }

        @Test
        @DisplayName("bubbles DuplicateLoginException then thrown")
        void bubblesDuplicateLogin() {
            // Given
            doThrow(new DuplicateLoginException("dupLogin"))
                    .when(repo).save(any(User.class));
            // When / Then
            assertThrows(DuplicateLoginException.class,
                    () -> sut.register(Role.USER, "D", "dup", "qwerty1"));
        }
    }

    @Nested
    @DisplayName("login(login, rawPassword)")
    class Login {

        @Test
        @DisplayName("returns user when password matches")
        void okWhenPasswordMatches() {
            // Given
            when(repo.findByLogin("john"))
                    .thenReturn(Optional.of(u("id1", "John", "john", "pass123")));
            // When
            Optional<User> got = sut.login("john", "pass123");
            // Then
            assertTrue(got.isPresent());
            assertEquals("id1", got.get().getUserId());
        }

        @Test
        @DisplayName("returns empty when password mismatch or user not found")
        void emptyWhenMismatchOrMissing() {
            // Given
            when(repo.findByLogin("john"))
                    .thenReturn(Optional.of(u("id1", "John", "john", "pass123")));
            // When
            Optional<User> a = sut.login("john", "wrong");
            Optional<User> b = sut.login("ghost", "any");
            // Then
            assertTrue(a.isEmpty());
            assertTrue(b.isEmpty());
        }
    }

    @Nested
    @DisplayName("finders delegation")
    class Finders {

        @Test
        @DisplayName("findByLogin delegates to repo")
        void findByLoginDelegates() {
            // Given
            when(repo.findByLogin("x")).thenReturn(Optional.of(u("id", "X", "x", "p")));
            // When / Then
            assertTrue(sut.findByLogin("x").isPresent());
            verify(repo).findByLogin("x");
        }

        @Test
        @DisplayName("findById delegates to repo")
        void findByIdDelegates() {
            // Given
            when(repo.findById("id")).thenReturn(Optional.of(u("id", "A", "a", "p")));
            // When / Then
            assertTrue(sut.findById("id").isPresent());
            verify(repo).findById("id");
        }

        @Test
        @DisplayName("findAll delegates to repo")
        void findAllDelegates() {
            // Given
            List<User> list = List.of(u("1", "A", "a", "p"));
            when(repo.findAll()).thenReturn(list);
            // When
            List<User> got = sut.findAll();
            // Then
            assertEquals(list, got);
            verify(repo).findAll();
        }
    }

    @Nested
    @DisplayName("updateProfile(userId, newName)")
    class UpdateProfile {

        @Test
        @DisplayName("throws when newName blank")
        void throwsOnBlank() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.updateProfile("id", "   "));
        }

        @Test
        @DisplayName("throws when user not found")
        void throwsWhenMissing() {
            // Given
            when(repo.findById("id")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(NoSuchElementException.class,
                    () -> sut.updateProfile("id", "Name"));
        }

        @Test
        @DisplayName("updates name and calls repo.update")
        void updatesName() {
            // Given
            User cur = u("id", "Old", "login", "pass");
            when(repo.findById("id")).thenReturn(Optional.of(cur));
            // When
            User updated = sut.updateProfile("id", "  New Name ");
            // Then
            ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
            verify(repo).update(cap.capture());
            assertEquals("New Name", cap.getValue().getUserName());
            assertEquals("New Name", updated.getUserName());
        }
    }

    @Nested
    @DisplayName("changePassword(userId, current, new)")
    class ChangePassword {

        @Test
        @DisplayName("throws when new blank or too short")
        void throwsOnBadNew() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.changePassword("id", "old", "  "));
            assertThrows(IllegalArgumentException.class,
                    () -> sut.changePassword("id", "old", "short"));
        }

        @Test
        @DisplayName("throws when user not found")
        void throwsWhenMissing() {
            // Given
            when(repo.findById("id")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(NoSuchElementException.class,
                    () -> sut.changePassword("id", "old", "newpass"));
        }

        @Test
        @DisplayName("throws when current password mismatch")
        void throwsOnWrongCurrent() {
            // Given
            when(repo.findById("id"))
                    .thenReturn(Optional.of(u("id", "A", "a", "secret")));
            // When / Then
            assertThrows(SecurityException.class,
                    () -> sut.changePassword("id", "oops", "newpass"));
        }

        @Test
        @DisplayName("updates when ok then repo.update called with new password")
        void updatesWhenOk() {
            // Given
            when(repo.findById("id"))
                    .thenReturn(Optional.of(u("id", "A", "a", "secret")));
            // When
            sut.changePassword("id", "secret", "newpass");
            // Then
            ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
            verify(repo).update(cap.capture());
            assertEquals("newpass", cap.getValue().getPassword());
        }
    }

    @Nested
    @DisplayName("adminUpdate(userId, role, name, login, newPasswordOrNull)")
    class AdminUpdate {

        @Test
        @DisplayName("ignores blank newPassword (non-null but blank) then keeps old password")
        void ignoresBlankNewPassword() {
            // Given
            User cur = User.of(Role.USER, "Old", "old", "oldpass").withId("id");
            when(repo.findById("id")).thenReturn(Optional.of(cur));
            // When
            User out = sut.adminUpdate("id", Role.ADMIN, "Name", "Login", "   ");
            // Then
            ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
            verify(repo).update(cap.capture());
            User upd = cap.getValue();
            assertEquals("oldpass", upd.getPassword());
            assertEquals("oldpass", out.getPassword());
            assertEquals(Role.ADMIN, upd.getRole());
            assertEquals("Name", upd.getUserName());
            assertEquals("login", upd.getUserLogin());
        }

        @Test
        @DisplayName("throws when user not found")
        void throwsWhenMissing() {
            // Given
            when(repo.findById("id")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(NoSuchElementException.class,
                    () -> sut.adminUpdate("id", Role.ADMIN, "Name", "login", null));
        }

        @Test
        @DisplayName("throws when name or login blank")
        void throwsWhenBlankNameOrLogin() {
            // Given
            when(repo.findById("id")).thenReturn(Optional.of(u("id", "A", "a", "p")));
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.adminUpdate("id", Role.ADMIN, "   ", "login", null));
            assertThrows(IllegalArgumentException.class,
                    () -> sut.adminUpdate("id", Role.ADMIN, "Name", "   ", null));
        }

        @Test
        @DisplayName("updates role/name/login (login lowercased) when valid and no password change")
        void updatesCoreFields() {
            // Given
            User cur = u("id", "Old", "old", "pass123");
            when(repo.findById("id")).thenReturn(Optional.of(cur));
            // When
            User out = sut.adminUpdate("id", Role.ADMIN, "  New Name  ", "NewLogin", null);
            // Then
            ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
            verify(repo).update(cap.capture());
            User upd = cap.getValue();
            assertEquals(Role.ADMIN, upd.getRole());
            assertEquals("New Name", upd.getUserName());
            assertEquals("newlogin", upd.getUserLogin());
            assertEquals("pass123", upd.getPassword());
            assertEquals("newlogin", out.getUserLogin());
        }

        @Test
        @DisplayName("throws when new password equals current")
        void throwsWhenSamePassword() {
            // Given
            when(repo.findById("id"))
                    .thenReturn(Optional.of(u("id", "A", "a", "samePass")));
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.adminUpdate("id", null, "Name", "login", "samePass"));
        }

        @Test
        @DisplayName("throws when new password too short")
        void throwsWhenPasswordTooShort() {
            // Given
            when(repo.findById("id"))
                    .thenReturn(Optional.of(u("id", "A", "a", "oldpass")));
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.adminUpdate("id", null, "Name", "login", "12345"));
        }

        @Test
        @DisplayName("sets new password when valid")
        void setsNewPasswordWhenValid() {
            // Given
            when(repo.findById("id"))
                    .thenReturn(Optional.of(u("id", "A", "a", "oldpass")));
            // When
            User out = sut.adminUpdate("id", null, "Name", "login", "newpass");
            // Then
            ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
            verify(repo).update(cap.capture());
            assertEquals("newpass", cap.getValue().getPassword());
            assertEquals("newpass", out.getPassword());
        }
    }
}