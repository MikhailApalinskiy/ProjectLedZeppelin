package com.javarush.apalinskiy.service;

import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.repositories.UserRepository;
import com.javarush.apalinskiy.user.Role;
import com.javarush.apalinskiy.user.User;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultUserServiceTest {

    @Mock
    UserRepository userRepository;
    @InjectMocks
    DefaultUserService defaultUserService;
    @Captor
    ArgumentCaptor<User> userCaptor;

    @Nested
    class Register {

        @Test
        void saveSucceedsOnceCallAndReturnsSameInstancePreservesFieldsTest() {
            // given
            // when
            User returned = defaultUserService.register(Role.USER, "User", "user", "user");
            // then
            verify(userRepository, times(1)).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertSame(returned, saved);
            assertAll(
                    () -> assertNotNull(returned.getUserId()),
                    () -> assertEquals(Role.USER, returned.getRole()),
                    () -> assertEquals("User", returned.getUserName()),
                    () -> assertEquals("user", returned.getUserLogin()),
                    () -> assertEquals("user", returned.getPassword())
            );
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void retriesOnceOnDuplicateIdThenSucceedsTest() {
            // given
            doThrow(new DuplicateIdException())
                    .doNothing()
                    .when(userRepository).save(any(User.class));
            // when
            User returned = defaultUserService.register(Role.USER, "User", "user", "user");
            // then
            verify(userRepository, times(2)).save(userCaptor.capture());
            List<User> attempts = userCaptor.getAllValues();
            assertEquals(2, attempts.size());
            assertNotEquals(attempts.get(0).getUserId(), attempts.get(1).getUserId());
            assertStableBetweenAttempts(attempts.get(0), attempts.get(1));
            assertSame(returned, attempts.get(1));
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void retriesTwiceOnDuplicateIdThenSucceedsTest() {
            // given
            doThrow(new DuplicateIdException())
                    .doThrow(new DuplicateIdException())
                    .doNothing()
                    .when(userRepository).save(any(User.class));
            // when
            User returned = defaultUserService.register(Role.USER, "User", "user", "user");
            // then
            verify(userRepository, times(3)).save(userCaptor.capture());
            List<User> attempts = userCaptor.getAllValues();
            assertEquals(3, attempts.size());
            assertNotEquals(attempts.get(0).getUserId(), attempts.get(1).getUserId());
            assertNotEquals(attempts.get(1).getUserId(), attempts.get(2).getUserId());
            assertStableBetweenAttempts(attempts.get(0), attempts.get(1));
            assertStableBetweenAttempts(attempts.get(1), attempts.get(2));
            assertSame(returned, attempts.get(2));
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void duplicateIdThreeTimesIllegalStateTest() {
            // given
            doThrow(new DuplicateIdException())
                    .doThrow(new DuplicateIdException())
                    .doThrow(new DuplicateIdException())
                    .when(userRepository).save(any(User.class));
            // when
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> defaultUserService.register(Role.USER, "User", "user", "user"));
            // then
            assertEquals("Failed to generate unique userId after retries", ex.getMessage());
            verify(userRepository, times(3)).save(userCaptor.capture());
            List<User> attempts = userCaptor.getAllValues();
            assertEquals(3, attempts.size());
            assertNotEquals(attempts.get(0).getUserId(), attempts.get(1).getUserId());
            assertNotEquals(attempts.get(1).getUserId(), attempts.get(2).getUserId());
            assertStableBetweenAttempts(attempts.get(0), attempts.get(1));
            assertStableBetweenAttempts(attempts.get(1), attempts.get(2));
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void duplicateLoginPropagatesWithoutRetriesTest() {
            // given
            doThrow(new DuplicateLoginException()).when(userRepository).save(any(User.class));
            // when / then
            assertThrows(DuplicateLoginException.class,
                    () -> defaultUserService.register(Role.USER, "User", "user", "user"));
            verify(userRepository, times(1)).save(any(User.class));
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void roleNullDefaultsToUserTest() {
            // given
            // when
            User u = defaultUserService.register(null, "User", "user", "user");
            // then
            assertEquals(Role.USER, u.getRole());
            verify(userRepository, times(1)).save(userCaptor.capture());
            assertEquals(Role.USER, userCaptor.getValue().getRole());
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void duplicateIdThenDuplicateLoginStopsRetryAndPropagatesDuplicateLoginTest() {
            // given
            doThrow(new DuplicateIdException())
                    .doThrow(new DuplicateLoginException())
                    .when(userRepository).save(any(User.class));
            // when / then
            assertThrows(DuplicateLoginException.class,
                    () -> defaultUserService.register(Role.USER, "User", "user", "user"));
            verify(userRepository, times(2)).save(userCaptor.capture());
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void arbitraryRuntimeExceptionIsNotRetriedAndPropagatesTest() {
            // given
            doThrow(new RuntimeException("boom")).when(userRepository).save(any(User.class));
            // when / then
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> defaultUserService.register(Role.USER, "User", "user", "user"));
            assertEquals("boom", ex.getMessage());
            verify(userRepository, times(1)).save(any(User.class));
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void createdAtIsStableAcrossRetriesOnDuplicateIdTest() {
            // given
            doThrow(new DuplicateIdException())
                    .doThrow(new DuplicateIdException())
                    .doNothing()
                    .when(userRepository).save(any(User.class));
            // when
            defaultUserService.register(Role.USER, "User", "user", "user");
            // then
            verify(userRepository, times(3)).save(userCaptor.capture());
            List<User> attempts = userCaptor.getAllValues();
            assertEquals(attempts.get(0).getCreatedAt(), attempts.get(1).getCreatedAt());
            assertEquals(attempts.get(1).getCreatedAt(), attempts.get(2).getCreatedAt());
            verifyNoMoreInteractions(userRepository);
        }

        private void assertStableBetweenAttempts(User a, User b) {
            assertEquals(a.getRole(), b.getRole(), "role must remain the same");
            assertEquals(a.getUserName(), b.getUserName(), "userName must remain the same");
            assertEquals(a.getUserLogin(), b.getUserLogin(), "userLogin must remain the same");
            assertEquals(a.getPassword(), b.getPassword(), "password must remain the same");
            assertEquals(a.getCreatedAt(), b.getCreatedAt(), "createdAt must remain the same");
        }
    }

    @Nested
    class Login {

        @Test
        void passwordMatchesReturnsSameUserAndFindCalledOnceTest() {
            // given
            User persisted = User.of(Role.USER, "User", "user", "pwd");
            when(userRepository.findByLogin("user")).thenReturn(Optional.of(persisted));
            // when
            Optional<User> actual = defaultUserService.login("user", "pwd");
            // then
            assertTrue(actual.isPresent());
            assertSame(persisted, actual.get());
            verify(userRepository, times(1)).findByLogin("user");
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void passwordDoesNotMatchReturnsEmptyTest() {
            // given
            User persisted = User.of(Role.USER, "User", "user", "pwd");
            when(userRepository.findByLogin("user")).thenReturn(Optional.of(persisted));
            // when
            Optional<User> actual = defaultUserService.login("user", "wrong");
            // then
            assertTrue(actual.isEmpty());
            verify(userRepository, times(1)).findByLogin("user");
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void userNotFoundReturnsEmptyTest() {
            // given
            when(userRepository.findByLogin("user")).thenReturn(Optional.empty());
            // when
            Optional<User> actual = defaultUserService.login("user", "any");
            // then
            assertTrue(actual.isEmpty());
            verify(userRepository, times(1)).findByLogin("user");
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void loginNullPropagatesNpeAndNoMoreInteractionsTest() {
            // given
            when(userRepository.findByLogin(null)).thenThrow(new NullPointerException("login"));
            // when / then
            assertThrows(NullPointerException.class, () -> defaultUserService.login(null, "pwd"));
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void passwordNullReturnsEmptyWhenUserFoundTest() {
            // given
            User persisted = User.of(Role.USER, "User", "user", "pwd");
            when(userRepository.findByLogin("user")).thenReturn(Optional.of(persisted));
            // when
            Optional<User> actual = defaultUserService.login("user", null);
            // then
            assertTrue(actual.isEmpty());
            verify(userRepository, times(1)).findByLogin("user");
            verifyNoMoreInteractions(userRepository);
        }
    }

    @Nested
    class FindByLogin {

        @Test
        void delegatesToRepositoryReturnsSameOptionalTest() {
            // given
            User persisted = User.of(Role.USER, "User", "user", "pwd");
            Optional<User> repoResult = Optional.of(persisted);
            when(userRepository.findByLogin("user")).thenReturn(repoResult);
            // when
            Optional<User> actual = defaultUserService.findByLogin("user");
            // then
            assertEquals(repoResult, actual);
            verify(userRepository, times(1)).findByLogin("user");
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void nullLoginPropagatesNpeTest() {
            // given
            when(userRepository.findByLogin(null)).thenThrow(new NullPointerException("login"));
            // when / then
            assertThrows(NullPointerException.class, () -> defaultUserService.findByLogin(null));
            verifyNoMoreInteractions(userRepository);
        }
    }
}