package com.javarush.apalinskiy.service.impl.social;

import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.social.FriendRequest;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.social.FriendRepository;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultFriendService")
class DefaultFriendServiceTest {

    @Mock
    FriendRepository repo;
    @Mock
    UserService users;
    @Mock
    NotificationService notify;

    DefaultFriendService sut;

    private User user(String id, String name) {
        return User.of(Role.USER, name, name.toLowerCase(), "p").withId(id);
    }

    @BeforeEach
    void setUp() {
        sut = new DefaultFriendService(repo, users, notify);
    }

    @Nested
    @DisplayName("sendRequest(from -> to)")
    class SendRequest {

        @Test
        @DisplayName("throws when from==to")
        void selfAddThrows() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class, () -> sut.sendRequest("u", "u"));
            verifyNoInteractions(repo, notify);
        }

        @Test
        @DisplayName("throws when target user not found")
        void targetNotFound() {
            // Given
            when(users.findById("to")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> sut.sendRequest("from", "to"));
        }

        @Test
        @DisplayName("throws when already friends")
        void alreadyFriends() {
            // Given
            when(users.findById("b")).thenReturn(Optional.of(user("b", "B")));
            when(repo.areFriends("a", "b")).thenReturn(true);
            // When / Then
            assertThrows(IllegalStateException.class, () -> sut.sendRequest("a", "b"));
        }

        @Test
        @DisplayName("auto-accepts when reverse pending exists (to->from)")
        void autoAcceptsOnReversePending() {
            // Given
            when(users.findById("to")).thenReturn(Optional.of(user("to", "To")));
            when(repo.areFriends("from", "to")).thenReturn(false);
            when(repo.findPending("to", "from")).thenReturn(Optional.of(mock(FriendRequest.class)));
            // When
            sut.sendRequest("from", "to");
            // Then
            verify(repo).removeRequest("to", "from");
            verify(repo).addFriendship("from", "to");
            verify(repo, never()).saveRequest(any());
            ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notify).notify(cap.capture());
            NotificationEvent e = cap.getValue();
            assertEquals(NotificationType.FRIEND_ACCEPTED, e.type());
            assertEquals("from", e.actorUserId());
            assertEquals("to", e.targetUserId());
        }

        @Test
        @DisplayName("throws when same-direction pending already exists (from->to)")
        void sameDirectionPendingThrows() {
            // Given
            when(users.findById("to")).thenReturn(Optional.of(user("to", "To")));
            when(repo.areFriends("from", "to")).thenReturn(false);
            when(repo.findPending("to", "from")).thenReturn(Optional.empty());
            when(repo.findPending("from", "to")).thenReturn(Optional.of(mock(FriendRequest.class)));
            // When / Then
            assertThrows(IllegalStateException.class, () -> sut.sendRequest("from", "to"));
        }

        @Test
        @DisplayName("creates pending and notifies FRIEND_REQUEST when no conflicts")
        void createsPendingAndNotifies() {
            // Given
            when(users.findById("to")).thenReturn(Optional.of(user("to", "To")));
            when(repo.areFriends("from", "to")).thenReturn(false);
            when(repo.findPending("to", "from")).thenReturn(Optional.empty());
            when(repo.findPending("from", "to")).thenReturn(Optional.empty());
            // When
            sut.sendRequest("from", "to");
            // Then
            verify(repo).saveRequest(any(FriendRequest.class));
            ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notify).notify(cap.capture());
            assertEquals(NotificationType.FRIEND_REQUEST, cap.getValue().type());
            assertEquals("from", cap.getValue().actorUserId());
            assertEquals("to", cap.getValue().targetUserId());
        }
    }

    @Nested
    @DisplayName("accept(to, from)")
    class Accept {

        @Test
        @DisplayName("throws when pending missing")
        void noPendingThrows() {
            // Given
            when(repo.findPending("from", "to")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(IllegalStateException.class, () -> sut.accept("to", "from"));
        }

        @Test
        @DisplayName("removes pending, adds friendship, notifies FRIEND_ACCEPTED")
        void acceptFlow() {
            // Given
            when(repo.findPending("from", "to")).thenReturn(Optional.of(mock(FriendRequest.class)));
            // When
            sut.accept("to", "from");
            // Then
            verify(repo).removeRequest("from", "to");
            verify(repo).addFriendship("from", "to");
            ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notify).notify(cap.capture());
            NotificationEvent e = cap.getValue();
            assertEquals(NotificationType.FRIEND_ACCEPTED, e.type());
            assertEquals("to", e.actorUserId());
            assertEquals("from", e.targetUserId());
        }
    }

    @Nested
    @DisplayName("decline(to, from)")
    class Decline {

        @Test
        @DisplayName("throws when pending missing")
        void noPendingThrows() {
            // Given
            when(repo.findPending("from", "to")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(IllegalStateException.class, () -> sut.decline("to", "from"));
        }

        @Test
        @DisplayName("removes pending only (no notification)")
        void removesOnly() {
            // Given
            when(repo.findPending("from", "to")).thenReturn(Optional.of(mock(FriendRequest.class)));
            // When
            sut.decline("to", "from");
            // Then
            verify(repo).removeRequest("from", "to");
            verifyNoInteractions(notify);
        }
    }

    @Nested
    @DisplayName("cancel(from, to)")
    class Cancel {

        @Test
        @DisplayName("throws when pending missing")
        void noPendingThrows() {
            // Given
            when(repo.findPending("from", "to")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(IllegalStateException.class, () -> sut.cancel("from", "to"));
        }

        @Test
        @DisplayName("removes pending only (no notification)")
        void removesOnly() {
            // Given
            when(repo.findPending("from", "to")).thenReturn(Optional.of(mock(FriendRequest.class)));
            // When
            sut.cancel("from", "to");
            // Then
            verify(repo).removeRequest("from", "to");
        }
    }

    @Nested
    @DisplayName("remove(user, friend)")
    class Remove {

        @Test
        @DisplayName("throws when not friends")
        void notFriendsThrows() {
            // Given
            when(repo.areFriends("u", "f")).thenReturn(false);
            // When / Then
            assertThrows(IllegalStateException.class, () -> sut.remove("u", "f"));
        }

        @Test
        @DisplayName("removes friendship when friends")
        void removesFriendship() {
            // Given
            when(repo.areFriends("u", "f")).thenReturn(true);
            // When
            sut.remove("u", "f");
            // Then
            verify(repo).removeFriendship("u", "f");
        }
    }

    @Nested
    @DisplayName("listFriends(user)")
    class ListFriends {

        @Test
        @DisplayName("returns only existing users (skips missing)")
        void returnsOnlyExistingUsers() {
            // Given
            when(repo.friendsOf("u")).thenReturn(Set.of("a", "missing"));
            when(users.findById("a")).thenReturn(Optional.of(user("a", "A")));
            when(users.findById("missing")).thenReturn(Optional.empty());
            // When
            List<User> list = sut.listFriends("u");
            // Then
            assertEquals(1, list.size());
            assertEquals("a", list.getFirst().getUserId());
        }
    }

    @Nested
    @DisplayName("incoming/outgoing")
    class Delegations {

        @Test
        @DisplayName("incoming delegates to repo")
        void incomingDelegates() {
            // Given
            List<FriendRequest> ret = List.of(mock(FriendRequest.class));
            when(repo.incoming("u")).thenReturn(ret);
            // When
            List<FriendRequest> list = sut.incoming("u");
            // Then
            assertEquals(ret, list);
            verify(repo).incoming("u");
        }

        @Test
        @DisplayName("outgoing delegates to repo")
        void outgoingDelegates() {
            // Given
            List<FriendRequest> ret = List.of(mock(FriendRequest.class), mock(FriendRequest.class));
            when(repo.outgoing("u")).thenReturn(ret);
            // When
            List<FriendRequest> list = sut.outgoing("u");
            // Then
            assertEquals(ret, list);
            verify(repo).outgoing("u");
        }
    }
}