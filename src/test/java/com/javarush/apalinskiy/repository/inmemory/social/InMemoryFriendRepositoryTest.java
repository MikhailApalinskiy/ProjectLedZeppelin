package com.javarush.apalinskiy.repository.inmemory.social;

import com.javarush.apalinskiy.domain.social.FriendRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryFriendRepository")
class InMemoryFriendRepositoryTest {

    private final InMemoryFriendRepository repo = new InMemoryFriendRepository();

    private FriendRequest req(String id, String from, String to, Instant t) throws Exception {
        Constructor<FriendRequest> c = FriendRequest.class.getDeclaredConstructor(
                String.class, String.class, String.class, Instant.class
        );
        c.setAccessible(true);
        return c.newInstance(id, from, to, t);
    }

    @Nested
    @DisplayName("areFriends()")
    class AreFriends {

        @Test
        @DisplayName("returns false when no friendship then false")
        void noFriendship() {
            // Given / When
            boolean res = repo.areFriends("a", "b");
            // Then
            assertFalse(res);
        }

        @Test
        @DisplayName("returns true after addFriendship then true both ways")
        void trueAfterAdd() {
            // Given
            repo.addFriendship("a", "b");
            // When / Then
            assertTrue(repo.areFriends("a", "b"));
            assertTrue(repo.areFriends("b", "a"));
        }
    }

    @Nested
    @DisplayName("addFriendship()")
    class AddFriendship {

        @Test
        @DisplayName("idempotent when called twice then uses v!=null on both sides and no duplicates")
        void idempotentOnSecondCall() {
            // Given
            repo.addFriendship("u1", "u2");
            // When
            repo.addFriendship("u1", "u2");
            // Then
            assertEquals(Set.of("u2"), repo.friendsOf("u1"));
            assertEquals(Set.of("u1"), repo.friendsOf("u2"));
        }

        @Test
        @DisplayName("adds to both sets when called then symmetric")
        void symmetricAdd() {
            // Given / When
            repo.addFriendship("u1", "u2");
            // Then
            assertEquals(Set.of("u2"), repo.friendsOf("u1"));
            assertEquals(Set.of("u1"), repo.friendsOf("u2"));
        }
    }

    @Nested
    @DisplayName("removeFriendship()")
    class RemoveFriendship {

        @Test
        @DisplayName("removes from both sets when existing then empty")
        void removesBoth() {
            // Given
            repo.addFriendship("x", "y");
            // When
            repo.removeFriendship("x", "y");
            // Then
            assertFalse(repo.areFriends("x", "y"));
            assertTrue(repo.friendsOf("x").isEmpty());
            assertTrue(repo.friendsOf("y").isEmpty());
        }

        @Test
        @DisplayName("no-op when user not present then no throw")
        void noopWhenMissing() {
            // Given
            repo.addFriendship("a", "b");
            // When / Then
            assertDoesNotThrow(() -> repo.removeFriendship("ghost", "a"));
            assertTrue(repo.areFriends("a", "b"));
        }
    }

    @Nested
    @DisplayName("friendsOf()")
    class FriendsOf {

        @Test
        @DisplayName("returns empty unmodifiable set when none then immutable empty")
        void emptyUnmodifiable() {
            // Given / When
            Set<String> s = repo.friendsOf("nouser");
            // Then
            assertTrue(s.isEmpty());
            assertThrows(UnsupportedOperationException.class, () -> s.add("x"));
        }
    }

    @Nested
    @DisplayName("pending requests: save/find/remove")
    class PendingRequests {

        @Test
        @DisplayName("saveRequest then findPending present when same pair then present")
        void saveThenFind() throws Exception {
            // Given
            FriendRequest r = req("r1", "a", "b", Instant.parse("2024-01-01T00:00:00Z"));
            // When
            repo.saveRequest(r);
            // Then
            assertTrue(repo.findPending("a", "b").isPresent());
        }

        @Test
        @DisplayName("removeRequest clears pending when exists then empty")
        void removeClears() throws Exception {
            // Given
            repo.saveRequest(req("r1", "a", "b", Instant.now()));
            // When
            repo.removeRequest("a", "b");
            // Then
            assertTrue(repo.findPending("a", "b").isEmpty());
        }

        @Test
        @DisplayName("second save from same sender overwrites when called then single latest kept")
        void secondSaveOverwrites() throws Exception {
            // Given
            Instant t1 = Instant.parse("2024-01-01T00:00:00Z");
            Instant t2 = Instant.parse("2024-01-02T00:00:00Z");
            repo.saveRequest(req("old", "a", "b", t1));
            // When
            repo.saveRequest(req("new", "a", "b", t2));
            // Then
            FriendRequest got = repo.findPending("a", "b").orElseThrow();
            assertEquals("new", got.getId());
            assertEquals(t2, got.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("incoming()")
    class Incoming {

        @Test
        @DisplayName("lists for target user sorted desc when multiple then newest first")
        void sortedDesc() throws Exception {
            // Given
            Instant t1 = Instant.parse("2024-01-01T00:00:00Z");
            Instant t2 = Instant.parse("2024-01-03T00:00:00Z");
            repo.saveRequest(req("r1", "a", "u", t1));
            repo.saveRequest(req("r2", "b", "u", t2));
            // When
            List<FriendRequest> list = repo.incoming("u");
            // Then
            assertEquals(List.of("r2", "r1"), list.stream().map(FriendRequest::getId).toList());
        }

        @Test
        @DisplayName("returns empty when no pending to user then empty")
        void emptyWhenNone() {
            // Given / When
            List<FriendRequest> list = repo.incoming("ghost");
            // Then
            assertTrue(list.isEmpty());
        }
    }

    @Nested
    @DisplayName("outgoing()")
    class Outgoing {

        @Test
        @DisplayName("lists requests sent by user across maps sorted desc then newest first")
        void collectsAcrossTargets() throws Exception {
            // Given
            Instant t1 = Instant.parse("2024-02-01T00:00:00Z");
            Instant t2 = Instant.parse("2024-02-02T00:00:00Z");
            Instant t3 = Instant.parse("2024-02-03T00:00:00Z");
            repo.saveRequest(req("x1", "me", "u1", t1));
            repo.saveRequest(req("x2", "me", "u2", t3));
            repo.saveRequest(req("x3", "other", "me", t2));
            // When
            List<FriendRequest> list = repo.outgoing("me");
            // Then
            assertEquals(List.of("x2", "x1"), list.stream().map(FriendRequest::getId).toList());
        }

        @Test
        @DisplayName("returns empty when no outgoing then empty")
        void emptyWhenNoOutgoing() {
            // Given / When
            List<FriendRequest> list = repo.outgoing("none");
            // Then
            assertTrue(list.isEmpty());
        }
    }
}