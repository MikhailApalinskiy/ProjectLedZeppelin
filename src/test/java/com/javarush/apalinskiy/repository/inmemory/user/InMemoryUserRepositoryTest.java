package com.javarush.apalinskiy.repository.inmemory.user;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryUserRepository")
class InMemoryUserRepositoryTest {

    private final InMemoryUserRepository repo = new InMemoryUserRepository();

    private User mk(String id, String name, String login, String password, Instant createdAt) throws Exception {
        Constructor<User> c = User.class.getDeclaredConstructor(
                Role.class, String.class, String.class, String.class, Instant.class, String.class
        );
        c.setAccessible(true);
        return c.newInstance(Role.USER, name, login, password, createdAt, id);
    }

    @Nested
    @DisplayName("findByLogin()")
    class FindByLogin {

        @Test
        @DisplayName("returns empty when nothing saved then empty")
        void emptyWhenNone() {
            // Given / When
            var res = repo.findByLogin("someone");
            // Then
            assertTrue(res.isEmpty());
        }

        @Test
        @DisplayName("normalizes login (trim+lower) when saved then found")
        void normalizesLogin() throws Exception {
            // Given
            repo.save(mk("u1", "Alice", "Admin", "p", Instant.parse("2024-01-01T00:00:00Z")));
            // When
            Optional<User> res = repo.findByLogin("  AdMiN  ");
            // Then
            assertTrue(res.isPresent());
            assertEquals("admin", res.get().getUserLogin());
        }
    }

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("saves new user when unique id and login then present")
        void savesUnique() throws Exception {
            // Given
            User u = mk("id1", "Bob", "bob", "p", Instant.now());
            // When
            repo.save(u);
            // Then
            assertTrue(repo.findById("id1").isPresent());
            assertTrue(repo.findByLogin("bob").isPresent());
        }

        @Test
        @DisplayName("throws DuplicateLoginException when login already used then error")
        void duplicateLogin() throws Exception {
            // Given
            repo.save(mk("id1", "A", "same", "p", Instant.now()));
            User u2 = mk("id2", "B", "same", "p2", Instant.now());
            // When / Then
            assertThrows(DuplicateLoginException.class, () -> repo.save(u2));
        }

        @Test
        @DisplayName("throws DuplicateIdException when id already used then error")
        void duplicateId() throws Exception {
            // Given
            repo.save(mk("idX", "A", "a_login", "p", Instant.now()));
            User u2 = mk("idX", "B", "other_login", "p2", Instant.now());
            // When / Then
            assertThrows(DuplicateIdException.class, () -> repo.save(u2));
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("allows change when new login already mapped to same id (occupiedBy==id)")
        void changeLoginWhenAliasAlreadyPointsToSameUser() throws Exception {
            // Given
            User u = mk("u1", "A", "old", "p", Instant.parse("2024-01-01T00:00:00Z"));
            repo.save(u);
            Field f = InMemoryUserRepository.class.getDeclaredField("idByLogin");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<String,String> map =
                    (ConcurrentHashMap<String,String>) f.get(repo);
            map.put("alias", "u1");
            User upd = u.withLogin("alias");
            // When
            assertDoesNotThrow(() -> repo.update(upd));
            // Then
            assertTrue(repo.findByLogin("alias").isPresent());
            assertTrue(repo.findByLogin("old").isEmpty());
            assertEquals("u1", repo.findByLogin("alias").orElseThrow().getUserId());
        }

        @Test
        @DisplayName("throws when user not found then NoSuchElementException")
        void notFound() throws Exception {
            // Given
            User ghost = mk("nope", "G", "g", "p", Instant.now());
            // When / Then
            assertThrows(NoSuchElementException.class, () -> repo.update(ghost));
        }

        @Test
        @DisplayName("updates fields when login unchanged then keep mapping")
        void updateSameLogin() throws Exception {
            // Given
            User u = mk("id1", "Old", "login", "p", Instant.now());
            repo.save(u);
            User upd = u.withUserName("New Name");
            // When
            repo.update(upd);
            // Then
            User got = repo.findByLogin("login").orElseThrow();
            assertEquals("New Name", got.getUserName());
            assertEquals("id1", got.getUserId());
        }

        @Test
        @DisplayName("changes login when free then old mapping removed and new present")
        void changeLoginWhenFree() throws Exception {
            // Given
            User u = mk("id1", "A", "old", "p", Instant.now());
            repo.save(u);
            User upd = u.withLogin("newLogin");
            // When
            repo.update(upd);
            // Then
            assertTrue(repo.findByLogin("newlogin").isPresent());
            assertTrue(repo.findByLogin("old").isEmpty());
        }

        @Test
        @DisplayName("throws DuplicateLoginException when changing login to occupied by another user")
        void changeLoginToOccupied() throws Exception {
            // Given
            repo.save(mk("u1", "A", "alpha", "p", Instant.now()));
            repo.save(mk("u2", "B", "beta", "p", Instant.now()));
            User u2New = mk("u2", "B", "ALPHA", "p", Instant.now());
            // When / Then
            assertThrows(DuplicateLoginException.class, () -> repo.update(u2New));
            assertTrue(repo.findByLogin("alpha").isPresent());
            assertTrue(repo.findByLogin("beta").isPresent());
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("sorted by createdAt DESC then by login ASC when same time")
        void sortedByCreatedThenLogin() throws Exception {
            // Given
            Instant t = Instant.parse("2024-01-01T00:00:00Z");
            repo.save(mk("a", "A", "charlie", "p", t));
            repo.save(mk("b", "B", "bravo", "p", t));
            Thread.sleep(2);
            repo.save(mk("c", "C", "alpha", "p", Instant.now()));
            // When
            List<User> all = repo.findAll();
            // Then
            assertEquals(List.of("c", "b", "a"),
                    all.stream().map(User::getUserId).toList());
        }

        @Test
        @DisplayName("returns unmodifiable list when returned then add throws")
        void unmodifiable() throws Exception {
            // Given
            repo.save(mk("x", "X", "x", "p", Instant.now()));
            // When
            List<User> all = repo.findAll();
            // Then
            assertThrows(UnsupportedOperationException.class, () -> all.add(all.getFirst()));
        }
    }
}