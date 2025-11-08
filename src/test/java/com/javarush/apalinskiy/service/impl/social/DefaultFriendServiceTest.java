package com.javarush.apalinskiy.service.impl.social;

import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.social.FriendRequest;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.social.FriendRepository;
import com.javarush.apalinskiy.service.impl.user.DefaultUserService;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultFriendService — unit tests (Given / When / Then)")
class DefaultFriendServiceTest {

    @Mock
    SessionFactory sessionFactory;
    @Mock
    Session session;
    @Mock
    Transaction tx;
    @Mock
    FriendRepository repo;
    @Mock
    UserService users;
    @Mock
    NotificationService notifySvc;

    private MockedStatic<HibernateUtil> mockedHibernate;
    private DefaultFriendService sut;

    @BeforeEach
    void init() {
        mockedHibernate = mockStatic(HibernateUtil.class);
        mockedHibernate.when(HibernateUtil::getSessionFactory).thenReturn(sessionFactory);
        when(sessionFactory.getCurrentSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(tx);
        sut = new DefaultFriendService(repo, users, notifySvc);
    }

    @AfterEach
    void cleanup() {
        mockedHibernate.close();
    }

    @Nested
    @DisplayName("sendRequest(from, to)")
    class SendRequest {

        @Test
        @DisplayName("throws IAE when from or to is null")
        void nullIds() {
            // Given
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> sut.sendRequest(null, "b"));
            assertThrows(IllegalArgumentException.class, () -> sut.sendRequest("a", null));
            verify(tx, times(2)).rollback();
        }

        @Test
        @DisplayName("throws IAE when from == to (self-add)")
        void selfAdd() {
            // Given
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> sut.sendRequest("u1", "u1"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("throws IAE when target user not found")
        void targetMissing() {
            // Given
            when(users.findById("to")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> sut.sendRequest("from", "to"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("throws ISE when already friends")
        void alreadyFriends() {
            // Given
            when(users.findById("to")).thenReturn(Optional.of(user("to", "To")));
            when(repo.areFriends("from", "to")).thenReturn(true);
            // When / Then
            assertThrows(IllegalStateException.class, () -> sut.sendRequest("from", "to"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("reverse pending — auto-accept: remove pending, add friendship, notify FRIEND_ACCEPTED, commit")
        void reversePending_autoAccept() {
            // Given
            when(users.findById("to")).thenReturn(Optional.of(user("to", "To")));
            when(repo.areFriends("from", "to")).thenReturn(false);
            when(repo.findPending("to", "from")).thenReturn(Optional.of(new FriendRequest()));
            // When
            sut.sendRequest("from", "to");
            // Then
            InOrder io = inOrder(repo, notifySvc, tx);
            io.verify(repo).removeRequest("to", "from");
            io.verify(repo).addFriendship("from", "to");
            ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
            io.verify(notifySvc).notify(cap.capture());
            io.verify(tx).commit();
            NotificationEvent ev = cap.getValue();
            assertEquals(NotificationType.FRIEND_ACCEPTED, ev.type());
            assertEquals("from", ev.actorUserId());
            assertEquals("to", ev.targetUserId());
        }

        @Test
        @DisplayName("already pending from->to — throws ISE")
        void alreadyPending_sameDirection() {
            // Given
            when(users.findById("to")).thenReturn(Optional.of(user("to", "To")));
            when(repo.areFriends("from", "to")).thenReturn(false);
            when(repo.findPending("to", "from")).thenReturn(Optional.empty());
            when(repo.findPending("from", "to")).thenReturn(Optional.of(new FriendRequest()));
            // When / Then
            assertThrows(IllegalStateException.class, () -> sut.sendRequest("from", "to"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("happy path — create request and notify FRIEND_REQUEST, commit")
        void happyPath_createsRequest_andNotifies() {
            // Given
            when(users.findById("to")).thenReturn(Optional.of(user("to", "To")));
            when(repo.areFriends("from", "to")).thenReturn(false);
            when(repo.findPending("to", "from")).thenReturn(Optional.empty());
            when(repo.findPending("from", "to")).thenReturn(Optional.empty());
            when(users.findById("from")).thenReturn(Optional.of(user("from", "From")));
            when(users.findById("to")).thenReturn(Optional.of(user("to", "To")));
            // When
            sut.sendRequest("from", "to");
            // Then
            verify(repo).saveRequest(any(FriendRequest.class));
            ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notifySvc).notify(cap.capture());
            verify(tx).commit();
            NotificationEvent ev = cap.getValue();
            assertEquals(NotificationType.FRIEND_REQUEST, ev.type());
            assertEquals("from", ev.actorUserId());
            assertEquals("to", ev.targetUserId());
        }
    }

    @Nested
    @DisplayName("accept(to, from)")
    class Accept {

        @Test
        @DisplayName("throws ISE when no pending request")
        void noPending() {
            // Given
            when(repo.findPending("from", "to")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(IllegalStateException.class, () -> sut.accept("to", "from"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("already friends — remove pending and commit (no-op)")
        void alreadyFriends_noop() {
            // Given
            when(repo.findPending("from", "to")).thenReturn(Optional.of(new FriendRequest()));
            when(repo.areFriends("from", "to")).thenReturn(true);
            // When
            sut.accept("to", "from");
            // Then
            verify(repo).removeRequest("from", "to");
            verify(tx).commit();
            verify(notifySvc, never()).notify(any());
            verify(repo, never()).addFriendship(anyString(), anyString());
        }

        @Test
        @DisplayName("happy path — remove request, add friendship, notify FRIEND_ACCEPTED, commit")
        void happyPath_accepts() {
            // Given
            when(repo.findPending("from", "to")).thenReturn(Optional.of(new FriendRequest()));
            when(repo.areFriends("from", "to")).thenReturn(false);
            // When
            sut.accept("to", "from");
            // Then
            InOrder io = inOrder(repo, notifySvc, tx);
            io.verify(repo).removeRequest("from", "to");
            io.verify(repo).addFriendship("from", "to");
            ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
            io.verify(notifySvc).notify(cap.capture());
            io.verify(tx).commit();
            NotificationEvent ev = cap.getValue();
            assertEquals(NotificationType.FRIEND_ACCEPTED, ev.type());
            assertEquals("to", ev.actorUserId());
            assertEquals("from", ev.targetUserId());
        }
    }

    @Nested
    @DisplayName("decline(to, from)")
    class Decline {

        @Test
        @DisplayName("throws ISE when no pending request")
        void noPending() {
            // Given
            when(repo.findPending("from", "to")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(IllegalStateException.class, () -> sut.decline("to", "from"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("happy path — remove request and commit")
        void happyPath() {
            // Given
            when(repo.findPending("from", "to")).thenReturn(Optional.of(new FriendRequest()));
            // When
            sut.decline("to", "from");
            // Then
            verify(repo).removeRequest("from", "to");
            verify(tx).commit();
        }
    }

    @Nested
    @DisplayName("cancel(from, to)")
    class Cancel {

        @Test
        @DisplayName("throws ISE when no pending request")
        void noPending() {
            // Given
            when(repo.findPending("from", "to")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(IllegalStateException.class, () -> sut.cancel("from", "to"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("happy path — remove request and commit")
        void happyPath() {
            // Given
            when(repo.findPending("from", "to")).thenReturn(Optional.of(new FriendRequest()));
            // When
            sut.cancel("from", "to");
            // Then
            verify(repo).removeRequest("from", "to");
            verify(tx).commit();
        }
    }

    @Nested
    @DisplayName("remove(userId, friendId)")
    class Remove {

        @Test
        @DisplayName("throws ISE when not friends")
        void notFriends() {
            // Given
            when(repo.areFriends("u", "f")).thenReturn(false);
            // When / Then
            assertThrows(IllegalStateException.class, () -> sut.remove("u", "f"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("happy path — remove friendship and commit")
        void happyPath() {
            // Given
            when(repo.areFriends("u", "f")).thenReturn(true);
            // When
            sut.remove("u", "f");
            // Then
            verify(repo).removeFriendship("u", "f");
            verify(tx).commit();
        }
    }

    @Nested
    @DisplayName("listFriends(userId)")
    class ListFriends {

        @Test
        @DisplayName("collects users by ids from repo.friendsOf and commits")
        void collectsUsers() {
            // Given
            var ordered = new java.util.LinkedHashSet<>(java.util.List.of("a", "b", "ghost"));
            when(repo.friendsOf("u")).thenReturn(ordered);
            when(users.findById("a")).thenReturn(Optional.of(user("a", "Alice")));
            when(users.findById("b")).thenReturn(Optional.of(user("b", "Bob")));
            when(users.findById("ghost")).thenReturn(Optional.empty());
            // When
            var out = sut.listFriends("u");
            // Then
            assertEquals(2, out.size());
            assertEquals("Alice", out.get(0).getUserName());
            assertEquals("Bob", out.get(1).getUserName());
            verify(tx).commit();
        }
    }

    @Nested
    @DisplayName("listFriendsPaged(userId, page, size, q)")
    class ListFriendsPaged {

        @Test
        @DisplayName("starts local tx, sets RO=true, calls count/page, commits, restores RO")
        void paged_ro_commit_restore() {
            // Given
            when(session.getTransaction()).thenReturn(tx);
            when(tx.isActive()).thenReturn(false);
            when(session.isDefaultReadOnly()).thenReturn(false);
            when(repo.countFriends("u", "al")).thenReturn(2L);
            when(repo.pageFriends("u", 1, 10, "al"))
                    .thenReturn(List.of(user("a", "Alice"), user("al", "Alfred")));
            // When
            DefaultUserService.PagedResult<User> pg = sut.listFriendsPaged("u", 1, 10, "al");
            // Then
            InOrder io = inOrder(session, repo, tx);
            io.verify(session).beginTransaction();
            io.verify(session).setDefaultReadOnly(true);
            io.verify(repo).countFriends("u", "al");
            io.verify(repo).pageFriends("u", 1, 10, "al");
            io.verify(tx).commit();
            io.verify(session).setDefaultReadOnly(false);
            assertEquals(2, pg.total());
            assertEquals(2, pg.items().size());
            assertEquals(1, pg.page());
            assertEquals(10, pg.size());
        }

        @Test
        @DisplayName("when total=0 — pageFriends is not called")
        void zeroTotal_skipsPage() {
            // Given
            when(session.getTransaction()).thenReturn(tx);
            when(tx.isActive()).thenReturn(false);
            when(session.isDefaultReadOnly()).thenReturn(false);
            when(repo.countFriends("u", null)).thenReturn(0L);
            // When
            DefaultUserService.PagedResult<User> pg = sut.listFriendsPaged("u", 2, 5, null);
            // Then
            verify(repo, never()).pageFriends(anyString(), anyInt(), anyInt(), any());
            verify(tx).commit();
            verify(session).setDefaultReadOnly(true);
            assertEquals(0, pg.total());
            assertTrue(pg.items().isEmpty());
            assertEquals(2, pg.page());
            assertEquals(5, pg.size());
        }
    }

    private static User user(String id, String name) {
        User u = new User();
        u.setUserId(id);
        u.setUserName(name);
        u.setUserLogin(id);
        return u;
    }
}