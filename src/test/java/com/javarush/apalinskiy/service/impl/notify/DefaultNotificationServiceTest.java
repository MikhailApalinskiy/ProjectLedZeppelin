package com.javarush.apalinskiy.service.impl.notify;

import com.javarush.apalinskiy.domain.notify.Notification;
import com.javarush.apalinskiy.domain.notify.NotificationEvent;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultNotificationService")
class DefaultNotificationServiceTest {

    @Mock
    NotificationRepository repo;

    @Mock
    UserService users;

    DefaultNotificationService sut;

    @BeforeEach
    void setUp() {
        sut = new DefaultNotificationService(repo, users);
    }

    private static NotificationEvent ev(NotificationType t, String actor, String target, Map<String, String> data) {
        return NotificationEvent.of(t, actor, target, data);
    }

    @Nested
    @DisplayName("add(userId,type,title,body)")
    class AddMethod {

        @Test
        @DisplayName("delegates to repo.save with built Notification then saved once")
        void delegatesToRepoSave() {
            // Given / When
            sut.add("u1", NotificationType.FRIEND_REQUEST, "t", "b");
            // Then
            ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
            verify(repo, times(1)).save(cap.capture());
            Notification n = cap.getValue();
            assertEquals("u1", n.getUserId());
            assertEquals(NotificationType.FRIEND_REQUEST, n.getType());
            assertEquals("t", n.getTitle());
            assertEquals("b", n.getBody());
            assertFalse(n.isRead());
        }
    }

    @Nested
    @DisplayName("notify(event)")
    class NotifyMethod {

        @Test
        @DisplayName("FRIEND_REQUEST uses actor name from users then formatted")
        void friendRequest_usesActorName() {
            // Given
            when(users.findById("a1")).thenReturn(Optional.of(User.of(Role.USER, "Alice", "alice", "p")));
            // When
            sut.notify(ev(NotificationType.FRIEND_REQUEST, "a1", "t1", Map.of()));
            // Then
            ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
            verify(repo).save(cap.capture());
            Notification n = cap.getValue();
            assertEquals("t1", n.getUserId());
            assertEquals("New friend request", n.getTitle());
            assertTrue(n.getBody().contains("<b>Alice</b>"));
            assertEquals(NotificationType.FRIEND_REQUEST, n.getType());
        }

        @Test
        @DisplayName("FRIEND_ACCEPTED falls back to 'User' when actor not found")
        void friendAccepted_unknownActor() {
            // Given
            when(users.findById("missing")).thenReturn(Optional.empty());
            // When
            sut.notify(ev(NotificationType.FRIEND_ACCEPTED, "missing", "t2", Map.of()));
            // Then
            ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
            verify(repo).save(cap.capture());
            Notification n = cap.getValue();
            assertEquals("The application has been accepted", n.getTitle());
            assertTrue(n.getBody().contains("<b>User</b> accepted"));
        }

        @Test
        @DisplayName("FRIEND_REMOVED uses 'System' when actorUserId is null")
        void friendRemoved_systemActor() {
            // Given / When
            sut.notify(ev(NotificationType.FRIEND_REMOVED, null, "t3", null));
            // Then
            ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
            verify(repo).save(cap.capture());
            Notification n = cap.getValue();
            assertEquals("Friend removed", n.getTitle());
            assertTrue(n.getBody().contains("<b>System</b> removed you"));
        }

        @Test
        @DisplayName("FRIEND_PUBLISHED_QUEST uses questName from data")
        void friendPublishedQuest_withName() {
            // Given
            when(users.findById("a")).thenReturn(Optional.of(User.of(Role.USER, "Bob", "bob", "p")));
            // When
            sut.notify(ev(NotificationType.FRIEND_PUBLISHED_QUEST, "a", "t4", Map.of("questName", "Dragon")));
            // Then
            ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
            verify(repo).save(cap.capture());
            Notification n = cap.getValue();
            assertEquals("A friend posted a quest", n.getTitle());
            assertTrue(n.getBody().contains("<b>Bob</b>"));
            assertTrue(n.getBody().contains("<b>Dragon</b>"));
        }

        @Test
        @DisplayName("FRIEND_PUBLISHED_QUEST defaults questName when absent")
        void friendPublishedQuest_defaultName() {
            // Given
            when(users.findById("a")).thenReturn(Optional.of(User.of(Role.USER, "C", "c", "p")));
            // When
            sut.notify(ev(NotificationType.FRIEND_PUBLISHED_QUEST, "a", "t5", Map.of()));
            // Then
            ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
            verify(repo).save(cap.capture());
            assertTrue(cap.getValue().getBody().contains("A new quest"));
        }

        @Test
        @DisplayName("QUEST_MODERATED uses questName and defaults result when absent")
        void questModerated_defaultsResult() {
            // Given
            when(users.findById("admin")).thenReturn(Optional.of(User.of(Role.ADMIN, "Admin", "root", "p")));
            // When
            sut.notify(ev(NotificationType.QUEST_MODERATED, "admin", "owner", Map.of("questName", "Q1")));
            // Then
            ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
            verify(repo).save(cap.capture());
            String body = cap.getValue().getBody();
            assertTrue(body.contains("Quest <b>Q1</b> passed moderation"));
            assertTrue(body.contains("<b>updated</b>"));
        }

        @Test
        @DisplayName("QUEST_ADMIN_CHANGED uses 'what' and questName")
        void questAdminChanged_usesWhat() {
            // Given
            when(users.findById("adm")).thenReturn(Optional.of(User.of(Role.ADMIN, "Root", "root", "p")));
            // When
            sut.notify(ev(NotificationType.QUEST_ADMIN_CHANGED, "adm", "owner", Map.of("questName", "Q2", "what", "Parameters updated")));
            // Then
            ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
            verify(repo).save(cap.capture());
            String body = cap.getValue().getBody();
            assertTrue(body.contains("Changes in <b>Q2</b>: Parameters updated."));
        }

        @Test
        @DisplayName("USER_ADMIN_CHANGED defaults 'what' when data null")
        void userAdminChanged_defaultWhat() {
            // Given / When
            sut.notify(ev(NotificationType.USER_ADMIN_CHANGED, "adm", "u", null));
            // Then
            ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
            verify(repo).save(cap.capture());
            String body = cap.getValue().getBody();
            assertTrue(body.contains("The administrator has updated your profile settings"));
            assertTrue(body.endsWith("."));
        }
    }
}