package com.javarush.apalinskiy.repositories;

import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.user.Role;
import com.javarush.apalinskiy.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserRepositoryTest {

    private InMemoryUserRepository repo;
    private User u;

    @BeforeEach
    void setUp() {
        u = User.of(Role.USER, "User", "user", "user");
        repo = new InMemoryUserRepository();
    }

    @Nested
    class Retrieval {

        @Test
        void savedUserIsTheSameByFindByIdTest() {
            // given
            repo.save(u);
            // when
            Optional<User> byId = repo.findById(u.getUserId());
            // then
            assertEquals(Optional.of(u), byId);
        }

        @Test
        void savesUserIsTheSameByFindByLoginTest() {
            // given
            repo.save(u);
            // when
            Optional<User> byLogin = repo.findByLogin(u.getUserLogin());
            // then
            assertEquals(Optional.of(u), byLogin);
        }

        @Test
        void byIdAndByLoginReturnTheSameUserTest() {
            // given
            repo.save(u);
            // when
            Optional<User> byLogin = repo.findByLogin(u.getUserLogin());
            Optional<User> byId = repo.findById(u.getUserId());
            // then
            assertEquals(byLogin, byId);
        }
    }

    @Nested
    class Save {

        @Test
        void nullInSaveThrowsNpeTest() {
            // given / when / then
            //noinspection DataFlowIssue
            assertThrows(NullPointerException.class, () -> repo.save(null));
        }

        @ParameterizedTest
        @ValueSource(strings = {"user", " USER ", "UsEr"})
        void duplicateLoginWithNormalizeThrowsDuplicateLoginTest(String params) {
            // given
            repo.save(u);
            // when
            DuplicateLoginException ex = assertThrows(DuplicateLoginException.class,
                    () -> repo.save(User.of(Role.USER, "User", params, "user")));
            // then
            assertEquals("Login already exists: " + u.getUserLogin(), ex.getMessage());
        }

        @Test
        void duplicateIdThrowsDuplicateIdTest() {
            // given
            repo.save(u);
            // when
            DuplicateIdException ex = assertThrows(DuplicateIdException.class,
                    () -> repo.save(User.of(Role.USER, "User", "userCopy", "user").withId(u.getUserId())));
            // then
            assertEquals("UserId already exists: " + u.getUserId(), ex.getMessage());
        }

        @Test
        void duplicateLoginCheckedBeforeDuplicateIdWhenBothSameTest() {
            // given
            repo.save(u);
            User u2 = User.of(Role.USER, "User", "user", "user").withId(u.getUserId());
            // when
            DuplicateLoginException ex = assertThrows(DuplicateLoginException.class, () -> repo.save(u2));
            // then
            assertEquals("Login already exists: " + u.getUserLogin(), ex.getMessage());
        }

        @Test
        void afterExceptionRepoStateUnchangedTest() {
            // given
            repo.save(u);
            User u2 = User.of(Role.USER, "User", "user", "user");
            // when
            assertThrows(DuplicateLoginException.class, () -> repo.save(u2));
            // then
            assertFalse(repo.findById(u2.getUserId()).isPresent());
            assertSame(u, repo.findByLogin(u2.getUserLogin()).orElseThrow());
        }

        @Test
        void savingSameInstanceTwiceThrowsDuplicateLoginTest() {
            // given
            repo.save(u);
            // when
            DuplicateLoginException ex = assertThrows(DuplicateLoginException.class, () -> repo.save(u));
            // then
            assertEquals("Login already exists: " + u.getUserLogin(), ex.getMessage());
        }
    }

    @Nested
    class FindByLogin {

        @Test
        void returnsEmptyWhenNotFoundTest() {
            // given / when
            Optional<User> res = repo.findByLogin("user");
            // then
            assertEquals(Optional.empty(), res);
        }

        @Test
        void returnsUserWhenFoundTest() {
            // given
            repo.save(u);
            // when
            Optional<User> res = repo.findByLogin("user");
            // then
            assertEquals(Optional.of(u), res);
        }

        @ParameterizedTest
        @ValueSource(strings = {" user ", "\tUsEr\n", "USER"})
        void trimsAndLowercasesLoginWhenSearchingTest(String params) {
            // given
            repo.save(u);
            // when / then
            assertEquals(Optional.of(u), repo.findByLogin(params));
        }

        @Test
        void nullLoginThrowsNpeTest() {
            // given / when / then
            //noinspection DataFlowIssue
            assertThrows(NullPointerException.class, () -> repo.findByLogin(null));
        }

        @Test
        void respectsLocaleRootLowercaseTurkishCaseTest() {
            // given
            User u2 = User.of(Role.USER, "User", "İI", "user");
            repo.save(u2);
            // when / then
            assertEquals(Optional.of(u2), repo.findByLogin("İI"));
        }
    }

    @Nested
    class FindById {

        @Test
        void returnsEmptyWhenNotFoundTest() {
            // given / when
            Optional<User> res = repo.findById(u.getUserId());
            // then
            assertEquals(Optional.empty(), res);
        }

        @Test
        void returnsUserWhenFoundTest() {
            // given
            repo.save(u);
            // when
            Optional<User> res = repo.findById(u.getUserId());
            // then
            assertEquals(Optional.of(u), res);
        }

        @Test
        void nullIdThrowsNpeTest() {
            // given / when / then
            assertThrows(NullPointerException.class, () -> repo.findById(null));
        }
    }

    @Nested
    class Concurrency {

        @Test
        void sameLoginOnlyOneSaveSucceedsTest() throws Exception {
            // given
            var u1 = User.of(Role.USER, "A", "user", "p");
            var u2 = User.of(Role.USER, "B", " USER ", "q");
            var start = new CountDownLatch(1);
            var ok = new AtomicInteger();
            Runnable r1 = () -> {
                // when
                await(start);
                try {
                    repo.save(u1);
                    ok.incrementAndGet();
                } catch (Exception ignore) {
                }
            };
            Runnable r2 = () -> {
                // when
                await(start);
                try {
                    repo.save(u2);
                    ok.incrementAndGet();
                } catch (Exception ignore) {
                }
            };
            // when
            var t1 = new Thread(r1);
            var t2 = new Thread(r2);
            t1.start();
            t2.start();
            start.countDown();
            t1.join();
            t2.join();
            // then
            assertEquals(1, ok.get());
            assertTrue(repo.findByLogin("user").isPresent());
        }

        @Test
        void sameIdOnlyOneSaveSucceedsTest() throws Exception {
            // given
            String id = UUID.randomUUID().toString();
            var u1 = User.of(Role.USER, "A", "a", "p").withId(id);
            var u2 = User.of(Role.USER, "B", "b", "q").withId(id);
            var start = new CountDownLatch(1);
            var ok = new AtomicInteger();
            Runnable r1 = () -> {
                // when
                await(start);
                try {
                    repo.save(u1);
                    ok.incrementAndGet();
                } catch (Exception ignore) {
                }
            };
            Runnable r2 = () -> {
                // when
                await(start);
                try {
                    repo.save(u2);
                    ok.incrementAndGet();
                } catch (Exception ignore) {
                }
            };
            // when
            var t1 = new Thread(r1);
            var t2 = new Thread(r2);
            t1.start();
            t2.start();
            start.countDown();
            t1.join();
            t2.join();
            // then
            assertEquals(1, ok.get());
            assertTrue(repo.findById(id).isPresent());
        }
    }

    private void await(CountDownLatch l) {
        try {
            l.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}