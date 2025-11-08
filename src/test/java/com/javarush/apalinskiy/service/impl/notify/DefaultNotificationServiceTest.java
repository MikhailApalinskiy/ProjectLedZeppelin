package com.javarush.apalinskiy.service.impl.notify;

import com.javarush.apalinskiy.domain.notify.Notification;
import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultNotificationService — unit tests with mocked Hibernate & deps")
class DefaultNotificationServiceTest {

    @Mock
    SessionFactory sessionFactory;
    @Mock
    Session session;
    @Mock
    Transaction tx;
    @Mock
    NotificationRepository repo;
    @Mock
    UserService users;

    private MockedStatic<HibernateUtil> mockedHibernateUtil;

    private DefaultNotificationService sut;

    @BeforeEach
    void initService() {
        mockedHibernateUtil = mockStatic(HibernateUtil.class);
        mockedHibernateUtil.when(HibernateUtil::getSessionFactory).thenReturn(sessionFactory);
        when(sessionFactory.getCurrentSession()).thenReturn(session);
        when(session.getTransaction()).thenReturn(tx);
        lenient().when(session.beginTransaction()).thenReturn(tx);
        lenient().when(session.isDefaultReadOnly()).thenReturn(true);
        sut = new DefaultNotificationService(repo, users);
    }

    @AfterEach
    void cleanupService() {
        if (mockedHibernateUtil != null) {
            mockedHibernateUtil.close();
        }
    }

    @Nested
    @DisplayName("add(userId, type, title, body)")
    class Add {

        @Test
        @DisplayName("starts local tx when inactive, persists notification, commits, restores RO")
        void happyPath_startsTx_andCommits() {
            // Given
            String uid = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
            User target = new User();
            target.setUserId(uid);
            target.setUserName("Alice");
            when(users.findById(uid)).thenReturn(Optional.of(target));
            when(tx.isActive()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            when(session.isDefaultReadOnly()).thenReturn(true);
            ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
            // When
            sut.add(uid, NotificationType.FRIEND_REQUEST, "New friend request", "Hi!");
            // Then
            verify(session).beginTransaction();
            verify(repo).save(cap.capture());
            verify(tx).commit();
            verify(session).setDefaultReadOnly(false);
            verify(session).setDefaultReadOnly(true);
            Notification n = cap.getValue();
            assertNotNull(n.getId(), "id is assigned");
            assertEquals(uid, n.getUser().getUserId());
            assertEquals(NotificationType.FRIEND_REQUEST, n.getType());
            assertEquals("New friend request", n.getTitle());
            assertEquals("Hi!", n.getBody());
            assertFalse(n.getRead());
            assertNotNull(n.getCreatedAt());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when user not found; rolls back and restores RO")
        void userMissing_throws_andRollsBack() {
            // Given
            String uid = "ghost";
            when(users.findById(uid)).thenReturn(Optional.empty());
            when(tx.isActive()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            when(session.isDefaultReadOnly()).thenReturn(true);
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.add(uid, NotificationType.FRIEND_REQUEST, "X", "Y"));
            verify(session).beginTransaction();
            verify(tx).rollback();
            verify(repo, never()).save(any());
            verify(session).setDefaultReadOnly(false);
            verify(session).setDefaultReadOnly(true);
        }

        @Test
        @DisplayName("does not start/commit tx when already active")
        void txAlreadyActive_noLocalStart() {
            // Given
            String uid = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
            User target = new User();
            target.setUserId(uid);
            target.setUserName("Alice");
            when(users.findById(uid)).thenReturn(Optional.of(target));
            when(tx.isActive()).thenReturn(true);
            // When
            sut.add(uid, NotificationType.USER_ADMIN_CHANGED, "Admin", "Profile updated");
            // Then
            verify(session, never()).beginTransaction();
            verify(tx, never()).commit();
            verify(repo).save(any(Notification.class));
            verify(session, never()).setDefaultReadOnly(anyBoolean());
        }
    }

    @Nested
    @DisplayName("notify(NotificationEvent)")
    class NotifyEvent {

        @Test
        @DisplayName("FRIEND_REQUEST uses actor name, composes title/body, and saves once")
        void friendRequest_textsAndSave() {
            // Given
            String actorId = "actor-1111-2222-3333-4444";
            String targetId = "target-aaaaaaaa-bbbb-cccc-dddd";
            User actor = new User();
            actor.setUserId(actorId);
            actor.setUserName("Bob");
            when(users.findById(actorId)).thenReturn(Optional.of(actor));
            when(tx.isActive()).thenReturn(false, true);
            User target = new User();
            target.setUserId(targetId);
            target.setUserName("Alice");
            when(users.findById(targetId)).thenReturn(Optional.of(target));
            ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
            NotificationEvent e = NotificationEvent.of(
                    NotificationType.FRIEND_REQUEST,
                    actorId,
                    targetId,
                    Map.of()
            );
            // When
            sut.notify(e);
            // Then
            verify(session).beginTransaction();
            verify(tx).commit();
            verify(repo).save(cap.capture());
            Notification n = cap.getValue();
            assertEquals(NotificationType.FRIEND_REQUEST, n.getType());
            assertEquals(targetId, n.getUser().getUserId());
            assertEquals("New friend request", n.getTitle());
            assertTrue(n.getBody().contains("Bob"), "Body must contain actor name");
        }

        @Test
        @DisplayName("FRIEND_PUBLISHED_QUEST uses questName from data; falls back if absent")
        void friendPublishedQuest_withQuestName() {
            // Given
            String actorId = "actor-1";
            String targetId = "target-1";
            User actor = new User();
            actor.setUserId(actorId);
            actor.setUserName("Carol");
            User target = new User();
            target.setUserId(targetId);
            target.setUserName("Dave");
            when(users.findById(actorId)).thenReturn(Optional.of(actor));
            when(users.findById(targetId)).thenReturn(Optional.of(target));
            when(tx.isActive()).thenReturn(false, true);
            ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
            NotificationEvent e = NotificationEvent.of(
                    NotificationType.FRIEND_PUBLISHED_QUEST,
                    actorId,
                    targetId,
                    Map.of("questName", "Demo Quest")
            );
            // When
            sut.notify(e);
            // Then
            verify(repo).save(cap.capture());
            Notification n = cap.getValue();
            assertEquals(NotificationType.FRIEND_PUBLISHED_QUEST, n.getType());
            assertEquals("A friend posted a quest", n.getTitle());
            assertTrue(n.getBody().contains("Carol"));
            assertTrue(n.getBody().contains("Demo Quest"));
        }

        @Test
        @DisplayName("when actorUserId is null, actorName becomes 'System'")
        void systemActor() {
            // Given
            String targetId = "target-x";
            User target = new User();
            target.setUserId(targetId);
            target.setUserName("Zed");
            when(users.findById(targetId)).thenReturn(Optional.of(target));
            when(tx.isActive()).thenReturn(false, true);
            ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
            NotificationEvent e = NotificationEvent.of(
                    NotificationType.FRIEND_REMOVED,
                    null,
                    targetId,
                    null
            );
            // When
            sut.notify(e);
            // Then
            verify(repo).save(cap.capture());
            assertTrue(cap.getValue().getBody().contains("System"));
        }

        @Test
        @DisplayName("propagates runtime exception from repo.save and rolls back outer tx")
        void repoFailure_rollsBack() {
            // Given
            String actorId = "actor-2";
            String targetId = "target-2";
            User actor = new User();
            actor.setUserId(actorId);
            actor.setUserName("Mike");
            User target = new User();
            target.setUserId(targetId);
            target.setUserName("Nina");
            when(users.findById(actorId)).thenReturn(Optional.of(actor));
            when(users.findById(targetId)).thenReturn(Optional.of(target));
            when(tx.isActive()).thenReturn(false, true);
            doThrow(new RuntimeException("DB is down")).when(repo).save(any());
            NotificationEvent e = NotificationEvent.of(
                    NotificationType.USER_ADMIN_CHANGED,
                    actorId,
                    targetId,
                    Map.of("what", "role updated")
            );
            // When / Then
            assertThrows(RuntimeException.class, () -> sut.notify(e));
            verify(tx).rollback();
        }
    }
}