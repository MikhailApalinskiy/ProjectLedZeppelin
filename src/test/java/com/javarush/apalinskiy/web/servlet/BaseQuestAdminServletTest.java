package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
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
            // when / then
            s.incCreatedByUserId("U3");
        }
    }

    @Nested
    @DisplayName("notifyFriendsPublished*")
    class NotifyFriends {

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