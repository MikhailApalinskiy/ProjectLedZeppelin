package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.notify.NotificationType;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.notify.NotificationService;
import com.javarush.apalinskiy.service.social.FriendService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendsServlet")
class FriendsServletTest {

    @Mock
    ServletConfig cfg;
    @Mock
    ServletContext ctx;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession ses;
    @Mock
    FriendService friendService;
    @Mock
    NotificationService notify;
    @Mock
    User me;
    @Mock
    User uA;
    @Mock
    User uB;

    private static void setField(Object target, String name, Object value) {
        try {
            Field f;
            try {
                f = target.getClass().getDeclaredField(name);
            } catch (NoSuchFieldException ex) {
                f = target.getClass().getSuperclass().getDeclaredField(name);
            }
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("given context has FriendService & NotificationService when init then ok")
        @SuppressWarnings("unused")
        void ok() {
            // given
            FriendsServlet s = new FriendsServlet();
            when(cfg.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.FRIEND_SERVICE)).thenReturn(friendService);
            when(ctx.getAttribute(WebConst.Ctx.NOTIFY_SERVICE)).thenReturn(notify);
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                // then
                assertDoesNotThrow(() -> s.init(cfg));
            }
        }

        @Test
        @DisplayName("given context has FriendService but NOTIFY missing when init then notify=null (no exception)")
        @SuppressWarnings("unused")
        void notifyMissing_isOptional() {
            // given
            FriendsServlet s = new FriendsServlet();
            when(cfg.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.FRIEND_SERVICE)).thenReturn(friendService);
            when(ctx.getAttribute(WebConst.Ctx.NOTIFY_SERVICE)).thenReturn(null);
            // when / then
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                assertDoesNotThrow(() -> s.init(cfg));
            }
        }

        @Test
        @DisplayName("given no FriendService when init then UnavailableException")
        @SuppressWarnings("unused")
        void noFriendService_fails() {
            // given
            FriendsServlet s = new FriendsServlet();
            when(cfg.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.FRIEND_SERVICE)).thenReturn(null);
            // when / then
            assertThrows(IllegalStateException.class, () -> {
                try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                    s.init(cfg);
                }
            });
        }
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @Test
        @DisplayName("given auth user when doGet then lists attached and forwards to JSP")
        void okForwards() throws Exception {
            // given
            FriendsServlet s = new FriendsServlet();
            setField(s, "service", friendService);
            setField(s, "notify", notify);
            when(req.getSession(false)).thenReturn(ses);
            when(req.getSession(true)).thenReturn(ses);
            when(ses.getAttribute(WebConst.Attr.OK)).thenReturn(null);
            when(ses.getAttribute(WebConst.Attr.ERROR)).thenReturn(null);
            when(ses.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(me.getUserId()).thenReturn("u1");
            when(friendService.listFriends("u1")).thenReturn(List.of(uA, uB));
            when(friendService.incoming("u1")).thenReturn(java.util.Collections.emptyList());
            when(friendService.outgoing("u1")).thenReturn(java.util.Collections.emptyList());
            RequestDispatcher rd = mock(RequestDispatcher.class);
            when(req.getRequestDispatcher(WebConst.Jsp.FRIENDS)).thenReturn(rd);
            // when
            s.doGet(req, resp);
            // then
            verify(req).setAttribute(eq("friends"),  eq(List.of(uA, uB)));
            verify(req).setAttribute(eq("incoming"), eq(java.util.Collections.emptyList()));
            verify(req).setAttribute(eq("outgoing"), eq(java.util.Collections.emptyList()));
            verify(rd).forward(req, resp);
        }

        @Test
        @DisplayName("given unauthenticated when doGet then redirect to login and return")
        void unauth_redirectLogin() throws Exception {
            // given
            FriendsServlet s = new FriendsServlet();
            setField(s, "service", friendService);
            when(req.getSession(false)).thenReturn(ses);
            when(req.getSession(true)).thenReturn(ses);
            when(ses.getAttribute(WebConst.Attr.OK)).thenReturn(null);
            when(ses.getAttribute(WebConst.Attr.ERROR)).thenReturn(null);
            when(ses.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.LOGIN), anyString()))
                        .then(inv -> null);
                s.doGet(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.LOGIN), eq("Please login")));
                verifyNoInteractions(friendService);
            }
        }
    }

    @Nested
    @DisplayName("doPost")
    class DoPost {

        private void authMe() {
            when(req.getSession(true)).thenReturn(ses);
            when(ses.getAttribute(WebConst.Attr.USER)).thenReturn(me);
        }

        @Test
        @DisplayName("given unauthenticated when doPost then early redirect to login (no service calls)")
        void unauth_early() throws Exception {
            // given
            FriendsServlet s = new FriendsServlet();
            setField(s, "service", friendService);
            when(req.getSession(true)).thenReturn(ses);
            when(ses.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.LOGIN), anyString()))
                        .then(inv -> null);
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.LOGIN), eq("Please login")));
                verifyNoInteractions(friendService);
            }
        }

        @Test
        @DisplayName("given action=request and id provided when doPost then sendRequest and redirectOk")
        void request_ok() throws Exception {
            // given
            FriendsServlet s = new FriendsServlet();
            setField(s, "service", friendService);
            authMe();
            when(me.getUserId()).thenReturn("me");
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("request");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("u2");
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), anyString()))
                        .then(inv -> null);
                s.doPost(req, resp);
                // then
                verify(friendService).sendRequest("me", "u2");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS),
                        eq("The application has been sent")));
            }
        }

        @Test
        @DisplayName("given action=request and id missing when doPost then redirectErr")
        void request_missingId() throws Exception {
            // given
            FriendsServlet s = new FriendsServlet();
            setField(s, "service", friendService);
            authMe();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("request");
            when(req.getParameter(WebConst.Param.ID)).thenReturn(null);
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), anyString()))
                        .then(inv -> null);
                s.doPost(req, resp);
                // then
                verifyNoInteractions(friendService);
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS),
                        eq("The recipient is not specified")));
            }
        }

        @Test
        @DisplayName("given action=accept and fromId provided when doPost then accept and redirectOk")
        void accept_ok() throws Exception {
            // given
            FriendsServlet s = new FriendsServlet();
            setField(s, "service", friendService);
            authMe();
            when(me.getUserId()).thenReturn("me");
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("accept");
            when(req.getParameter(WebConst.Param.FROM_ID)).thenReturn("u2");
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), anyString()))
                        .then(inv -> null);
                s.doPost(req, resp);
                // then
                verify(friendService).accept("me", "u2");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS),
                        eq("The application has been accepted")));
            }
        }

        @Test
        @DisplayName("given action=accept and fromId missing when doPost then redirectErr")
        void accept_missingFrom() throws Exception {
            // given
            FriendsServlet s = new FriendsServlet();
            setField(s, "service", friendService);
            authMe();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("accept");
            when(req.getParameter(WebConst.Param.FROM_ID)).thenReturn(null);
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), anyString()))
                        .then(inv -> null);
                s.doPost(req, resp);
                // then
                verifyNoInteractions(friendService);
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS),
                        eq("The sender is not specified")));
            }
        }

        @Test
        @DisplayName("given action=decline and fromId provided when doPost then decline and redirectOk")
        void decline_ok() throws Exception {
            // given
            FriendsServlet s = new FriendsServlet();
            setField(s, "service", friendService);
            authMe();
            when(me.getUserId()).thenReturn("me");
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("decline");
            when(req.getParameter(WebConst.Param.FROM_ID)).thenReturn("u2");
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), anyString()))
                        .then(inv -> null);
                s.doPost(req, resp);
                // then
                verify(friendService).decline("me", "u2");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS),
                        eq("The application was rejected")));
            }
        }

        @Test
        @DisplayName("given action=cancel and id provided when doPost then cancel and redirectOk")
        void cancel_ok() throws Exception {
            // given
            FriendsServlet s = new FriendsServlet();
            setField(s, "service", friendService);
            authMe();
            when(me.getUserId()).thenReturn("me");
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("cancel");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("u2");
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), anyString()))
                        .then(inv -> null);
                s.doPost(req, resp);
                // then
                verify(friendService).cancel("me", "u2");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS),
                        eq("The application has been cancelled")));
            }
        }

        @Test
        @DisplayName("given action=remove and friendId provided when doPost then remove, notify (if present), redirectOk")
        void remove_ok_notifies() throws Exception {
            // given
            FriendsServlet s = new FriendsServlet();
            setField(s, "service", friendService);
            setField(s, "notify", notify);
            authMe();
            when(me.getUserId()).thenReturn("me");
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("remove");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("friendX");
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), anyString()))
                        .then(inv -> null);
                s.doPost(req, resp);
                // then
                verify(friendService).remove("me", "friendX");
                verify(notify).notify(argThat(ev ->
                        ev.type() == NotificationType.FRIEND_REMOVED &&
                                "me".equals(ev.actorUserId()) &&
                                "friendX".equals(ev.targetUserId())
                ));
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS),
                        eq("The user has been removed from friends")));
            }
        }

        @Test
        @DisplayName("given action=remove and notify throws when doPost then still redirectOk (swallowed)")
        void remove_notifyThrows_swallowed() throws Exception {
            // given
            FriendsServlet s = new FriendsServlet();
            setField(s, "service", friendService);
            setField(s, "notify", notify);
            authMe();
            when(me.getUserId()).thenReturn("me");
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("remove");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("friendX");
            doThrow(new RuntimeException("boom")).when(notify).notify(any());
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), anyString()))
                        .then(inv -> null);
                s.doPost(req, resp);
                // then
                verify(friendService).remove("me", "friendX");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.FRIENDS),
                        eq("The user has been removed from friends")));
            }
        }

        @Test
        @DisplayName("given action unknown when doPost then redirectErr")
        void unknownAction() throws Exception {
            // given
            FriendsServlet s = new FriendsServlet();
            setField(s, "service", friendService);
            authMe();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("wtf");
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), anyString()))
                        .then(inv -> null);
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("Unknown action")));
                verifyNoInteractions(friendService);
            }
        }

        @Test
        @DisplayName("given action null when doPost then redirectErr")
        void nullAction() throws Exception {
            // given
            FriendsServlet s = new FriendsServlet();
            setField(s, "service", friendService);
            authMe();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn(null);
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), anyString()))
                        .then(inv -> null);
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("Unknown action")));
                verifyNoInteractions(friendService);
            }
        }

        @Test
        @DisplayName("given service throws inside switch when doPost then servletContext.log and redirectErr")
        void serviceThrows_logged_and_redirected() throws Exception {
            // given
            FriendsServlet real = new FriendsServlet();
            FriendsServlet s = spy(real);
            setField(s, "service", friendService);
            doReturn(ctx).when(s).getServletContext();
            authMe();
            when(me.getUserId()).thenReturn("me");
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("request");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("u2");
            doThrow(new RuntimeException("boom")).when(friendService).sendRequest("me", "u2");
            // when
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), anyString()))
                        .then(inv -> null);
                s.doPost(req, resp);
                // then
                verify(ctx).log(eq("Friends POST error"), any(RuntimeException.class));
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.FRIENDS), eq("boom")));
            }
        }
    }
}