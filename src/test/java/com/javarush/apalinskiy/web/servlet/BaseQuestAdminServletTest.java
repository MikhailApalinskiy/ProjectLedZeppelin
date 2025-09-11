package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.social.FriendService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.service.user.UserStatsService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.*;

@DisplayName("BaseQuestAdminServlet")
@ExtendWith(MockitoExtension.class)
class BaseQuestAdminServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    QuestAuthoringService authoring;
    @Mock
    NotificationService notify;
    @Mock
    UserService users;
    @Mock
    UserStatsService userStats;
    @Mock
    FriendService friendService;
    @Mock
    User admin;
    @Mock
    User userEntity;

    static class TestServlet extends BaseQuestAdminServlet {
    }

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("given all required beans exist when init then fields set; optional may be null")
        void init_ok() throws Exception {
            // given
            TestServlet s = new TestServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenReturn(authoring);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_SERVICE, NotificationService.class))
                        .thenReturn(notify);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(users);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class))
                        .thenReturn(userStats);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.FRIEND_SERVICE, FriendService.class))
                        .thenThrow(new IllegalStateException("no friendService"));
                // when
                s.init(config);
                // then
                assertEquals(authoring, s.authoring);
                assertEquals(notify, s.notify);
                assertEquals(users, s.users);
                assertEquals(userStats, s.userStats);
                Assertions.assertNull(s.friendService);
            }
        }

        @Test
        @DisplayName("given missing required bean when init then UnavailableException")
        void init_missing_required() {
            // given
            TestServlet s = new TestServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenThrow(new IllegalStateException("no authoring"));
                // when / then
                assertThrows(UnavailableException.class, () -> s.init(config));
            }
        }
    }

    @Nested
    @DisplayName("resolveUserIdByLogin")
    class ResolveUserIdByLogin {

        @Test
        @DisplayName("given null/blank or users=null when resolve then null")
        void null_inputs() {
            // given
            TestServlet s = new TestServlet();
            s.users = null;
            // when / then
            Assertions.assertNull(s.resolveUserIdByLogin(null));
            Assertions.assertNull(s.resolveUserIdByLogin(" "));
        }

        @Test
        @DisplayName("given existing login when resolve then returns userId")
        void ok() {
            // given
            TestServlet s = new TestServlet();
            s.users = users;
            when(users.findByLogin("john")).thenReturn(Optional.of(userEntity));
            when(userEntity.getUserId()).thenReturn("U1");
            // when
            String id = s.resolveUserIdByLogin("john");
            // then
            assertEquals("U1", id);
        }

        @Test
        @DisplayName("given missing login when resolve then null")
        void missing_user() {
            // given
            TestServlet s = new TestServlet();
            s.users = users;
            when(users.findByLogin("absent")).thenReturn(Optional.empty());
            // when / then
            Assertions.assertNull(s.resolveUserIdByLogin("absent"));
        }
    }

    @Nested
    @DisplayName("notifyQuestAdminChanged")
    class NotifyQuestAdminChanged {

        @Test
        @DisplayName("given null admin/notify/login when notify then no calls")
        void guards() {
            // given
            TestServlet s1 = new TestServlet();
            s1.notify = notify;
            s1.users = users;
            // when
            s1.notifyQuestAdminChanged(null, "owner", "Q");
            // then
            verify(notify, never()).notify(any());
            // given
            TestServlet s2 = new TestServlet();
            s2.notify = null;
            s2.users = users;
            // when
            s2.notifyQuestAdminChanged(admin, "owner", "Q");
            // then
            verifyNoInteractions(notify);
            // given
            TestServlet s3 = new TestServlet();
            s3.notify = notify;
            s3.users = users;
            // when
            s3.notifyQuestAdminChanged(admin, null, "Q");
            // then
            verify(notify, never()).notify(any());
        }

        @Test
        @DisplayName("given login not resolvable when notify then nothing happens")
        void login_not_found() {
            // given
            TestServlet s = new TestServlet();
            s.notify = notify;
            s.users = users;
            when(users.findByLogin("own")).thenReturn(Optional.empty());
            // when
            s.notifyQuestAdminChanged(admin, "own", "Q");
            // then
            verify(notify, never()).notify(any());
        }

        @Test
        @DisplayName("given valid inputs when notify then NotificationService.notify called")
        void ok() {
            // given
            TestServlet s = new TestServlet();
            s.notify = notify;
            s.users = users;
            when(users.findByLogin("own")).thenReturn(Optional.of(userEntity));
            when(userEntity.getUserId()).thenReturn("TGT");
            when(admin.getUserId()).thenReturn("ADM");
            // when
            s.notifyQuestAdminChanged(admin, "own", "QuestName");
            // then
            verify(notify, atLeastOnce()).notify(any());
        }

        @Test
        @DisplayName("given blank questName when notify then uses default 'Quest'")
        void default_questname() {
            // given
            TestServlet s = new TestServlet();
            s.notify = notify;
            s.users = users;
            when(users.findByLogin("own")).thenReturn(Optional.of(userEntity));
            when(userEntity.getUserId()).thenReturn("TGT");
            when(admin.getUserId()).thenReturn("ADM");
            // when
            s.notifyQuestAdminChanged(admin, "own", "  ");
            // then
            verify(notify, atLeastOnce()).notify(any());
        }
    }

    @Nested
    @DisplayName("incCreated*")
    class IncCreated {

        @Test
        @DisplayName("given null stats or null/blank userId when incCreatedByUserId then no calls")
        void guards_userId() {
            // given
            TestServlet s = new TestServlet();
            s.userStats = null;
            // when
            s.incCreatedByUserId(null);
            s.incCreatedByUserId(" ");
            // then
            verifyNoInteractions(userStats);
        }

        @Test
        @DisplayName("given valid userId when incCreatedByUserId then increments")
        void ok_userId() {
            // given
            TestServlet s = new TestServlet();
            s.userStats = userStats;
            // when
            s.incCreatedByUserId("U2");
            // then
            verify(userStats).incCreated("U2");
        }

        @Test
        @DisplayName("given stats throws when incCreatedByUserId then exception swallowed")
        void swallow_exceptions() {
            // given
            TestServlet s = new TestServlet();
            s.userStats = userStats;
            doThrow(new RuntimeException("boom")).when(userStats).incCreated("U3");
            // when / then (не падает)
            s.incCreatedByUserId("U3");
        }

        @Test
        @DisplayName("given login resolves to id when incCreatedByLogin then increments by resolved id")
        void ok_login() {
            // given
            TestServlet s = new TestServlet();
            s.users = users;
            s.userStats = userStats;
            when(users.findByLogin("bob")).thenReturn(Optional.of(userEntity));
            when(userEntity.getUserId()).thenReturn("U10");
            // when
            s.incCreatedByLogin("bob");
            // then
            verify(userStats).incCreated("U10");
        }

        @Test
        @DisplayName("given login null or not resolvable when incCreatedByLogin then no increments")
        void login_not_found() {
            // given
            TestServlet s = new TestServlet();
            s.users = users;
            s.userStats = userStats;
            // when
            s.incCreatedByLogin(null);
            when(users.findByLogin("x")).thenReturn(Optional.empty());
            s.incCreatedByLogin("x");
            // then
            verifyNoInteractions(userStats);
        }
    }

    @Nested
    @DisplayName("notifyFriendsPublished*")
    class NotifyFriends {

        @Test
        @DisplayName("given missing deps or login when byLogin then no notifications")
        void byLogin_guards() {
            // given
            TestServlet s1 = new TestServlet();
            s1.notify = notify;
            s1.friendService = friendService;
            s1.users = users;
            // when
            s1.notifyFriendsPublishedByLogin(null, "Q");
            // then
            verify(notify, never()).notify(any());
            // given
            TestServlet s2 = new TestServlet();
            s2.notify = null;
            s2.friendService = friendService;
            s2.users = users;
            // when
            s2.notifyFriendsPublishedByLogin("own", "Q");
            verifyNoInteractions(notify);
            // given
            TestServlet s3 = new TestServlet();
            s3.notify = notify;
            s3.friendService = null;
            s3.users = users;
            // when
            s3.notifyFriendsPublishedByLogin("own", "Q");
            verify(notify, never()).notify(any());
        }

        @Test
        @DisplayName("given login not resolvable when byLogin then no notifications")
        void byLogin_not_resolvable() {
            // given
            TestServlet s = new TestServlet();
            s.notify = notify;
            s.friendService = friendService;
            s.users = users;
            when(users.findByLogin("own")).thenReturn(Optional.empty());
            // when
            s.notifyFriendsPublishedByLogin("own", "Q");
            // then
            verify(notify, never()).notify(any());
        }

        @Test
        @DisplayName("given valid login when byLogin then delegates to byUserId")
        void byLogin_delegates() {
            // given
            TestServlet s = spy(new TestServlet());
            s.notify = notify;
            s.friendService = friendService;
            s.users = users;
            when(users.findByLogin("own")).thenReturn(Optional.of(userEntity));
            when(userEntity.getUserId()).thenReturn("OID");
            // when
            s.notifyFriendsPublishedByLogin("own", "Name");
            // then
            verify(s).notifyFriendsPublishedByUserId("OID", "Name");
        }

        @Test
        @DisplayName("given missing deps or ownerId=null when byUserId then no notifications")
        void byUserId_guards() {
            // given
            TestServlet s1 = new TestServlet();
            s1.notify = notify;
            s1.friendService = friendService;
            // when
            s1.notifyFriendsPublishedByUserId(null, "Q");
            // then
            verify(notify, never()).notify(any());
            // given
            TestServlet s2 = new TestServlet();
            s2.notify = null;
            s2.friendService = friendService;
            s2.notifyFriendsPublishedByUserId("O1", "Q");
            verifyNoInteractions(friendService);
            // given
            TestServlet s3 = new TestServlet();
            s3.notify = notify;
            s3.friendService = null;
            s3.notifyFriendsPublishedByUserId("O1", "Q");
            verify(notify, never()).notify(any());
        }

        @Test
        @DisplayName("given friendService throws when byUserId then swallowed")
        void byUserId_service_throws() {
            // given
            TestServlet s = new TestServlet();
            s.notify = notify;
            s.friendService = friendService;
            when(friendService.listFriendIds("O1")).thenThrow(new RuntimeException("boom"));
            // when
            s.notifyFriendsPublishedByUserId("O1", "Q");
            // then
            verify(notify, never()).notify(any());
        }

        @Test
        @DisplayName("given empty or null friend list when byUserId then no notifications")
        void byUserId_empty() {
            // given
            TestServlet s = new TestServlet();
            s.notify = notify;
            s.friendService = friendService;
            when(friendService.listFriendIds("O1")).thenReturn(List.of());
            s.notifyFriendsPublishedByUserId("O1", "Q");
            when(friendService.listFriendIds("O2")).thenReturn(null);
            s.notifyFriendsPublishedByUserId("O2", "Q");
            // then
            verify(notify, never()).notify(any());
        }

        @Test
        @DisplayName("given friend ids including null/blank/self when byUserId then notify only valid others")
        void byUserId_valid_only() {
            // given
            TestServlet s = new TestServlet();
            s.notify = notify;
            s.friendService = friendService;
            when(friendService.listFriendIds("O1"))
                    .thenReturn(Arrays.asList(null, " ", "O1", "F1", "F2"));
            // when
            s.notifyFriendsPublishedByUserId("O1", "Name");
            // then
            verify(notify, times(2)).notify(any());
        }

        @Test
        @DisplayName("given blank questName when byUserId then still notifies (default name)")
        void byUserId_default_name() {
            // given
            TestServlet s = new TestServlet();
            s.notify = notify;
            s.friendService = friendService;

            when(friendService.listFriendIds("O1"))
                    .thenReturn(List.of("F1"));
            // when
            s.notifyFriendsPublishedByUserId("O1", "  ");
            // then
            verify(notify, times(1)).notify(any());
        }
    }
}