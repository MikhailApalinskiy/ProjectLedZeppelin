package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.notify.Notification;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.repository.notify.NotificationRepository;
import com.javarush.apalinskiy.web.util.Web;
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

import java.util.List;

import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationsServlet")
class NotificationsServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;
    @Mock
    NotificationRepository repo;
    @Mock
    User me;
    @Mock
    Notification n1;
    @Mock
    Notification n2;

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("given ctx has repo when init then grabs bean")
        void ok() throws Exception {
            // given
            NotificationsServlet s = new NotificationsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                // when
                s.init(config);
                // then
                web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class));
            }
        }

        @Test
        @DisplayName("given no repo in ctx when init then IllegalStateException")
        void noRepo() {
            // given
            NotificationsServlet s = new NotificationsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            // when/then
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenThrow(new IllegalStateException("Context bean not found"));
                org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> s.init(config));
            }
        }
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @Test
        @DisplayName("given unauth when doGet then redirect to login and return")
        void unauth_redirectLogin() throws Exception {
            // given
            NotificationsServlet s = new NotificationsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                s.init(config);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.OK)).then(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).then(inv -> null);
                when(req.getSession(true)).thenReturn(session);
                when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.LOGIN), anyString()))
                        .then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                web.verify(() -> Web.pullFlash(req, WebConst.Attr.OK));
                web.verify(() -> Web.pullFlash(req, WebConst.Attr.ERROR));
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.LOGIN), eq("Please login")));
                verifyNoInteractions(repo);
            }
        }

        @Test
        @DisplayName("given user and params when doGet then query repo, set attrs, forward to JSP")
        void ok_forwards() throws Exception {
            // given
            NotificationsServlet s = new NotificationsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(req.getParameter("limit")).thenReturn("20");
            when(req.getParameter("offset")).thenReturn("5");
            when(req.getSession(true)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(me.getUserId()).thenReturn("u1");
            List<Notification> items = List.of(n1, n2);
            when(repo.list("u1", 20, 5)).thenReturn(items);
            when(repo.unreadCount("u1")).thenReturn(7);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                web.when(() -> Web.parseIntOrDefault("20", 50)).thenReturn(20);
                web.when(() -> Web.parseIntOrDefault("5", 0)).thenReturn(5);
                s.init(config);
                // when
                s.doGet(req, resp);
                // then
                verify(repo).list("u1", 20, 5);
                verify(repo).unreadCount("u1");
                verify(req).setAttribute("items", items);
                verify(req).setAttribute("unread", 7);
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.NOTIFICATIONS)));
                web.verify(() -> Web.pullFlash(eq(req), eq(WebConst.Attr.OK)));
                web.verify(() -> Web.pullFlash(eq(req), eq(WebConst.Attr.ERROR)));
            }
        }
    }

    @Nested
    @DisplayName("doPost")
    class DoPost {

        private void authMe() {
            when(req.getSession(true)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(me.getUserId()).thenReturn("u1");
        }

        @Test
        @DisplayName("given unauth when doPost then redirect login")
        void unauth() throws Exception {
            // given
            NotificationsServlet s = new NotificationsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                s.init(config);
                when(req.getSession(true)).thenReturn(session);
                when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.LOGIN), anyString()))
                        .then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.LOGIN), eq("Please login")));
                verifyNoInteractions(repo);
            }
        }

        @Test
        @DisplayName("given action=markRead with id when doPost then repo.markRead and redirectOk")
        void markRead_withId() throws Exception {
            // given
            NotificationsServlet s = new NotificationsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                s.init(config);
                authMe();
                when(req.getParameter(WebConst.Param.ACTION)).thenReturn("markRead");
                when(req.getParameter(WebConst.Param.ID)).thenReturn("n1");
                web.when(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.NOTIFICATIONS), anyString()))
                        .then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                verify(repo).markRead("u1", "n1");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.NOTIFICATIONS), eq("Marked as read")));
            }
        }

        @Test
        @DisplayName("given action=markRead without id when doPost then no repo call and redirectOk")
        void markRead_noId() throws Exception {
            // given
            NotificationsServlet s = new NotificationsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(req.getSession(true)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("markRead");
            when(req.getParameter(WebConst.Param.ID)).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                s.init(config);
                // when
                s.doPost(req, resp);
                // then
                verify(repo, never()).markRead(anyString(), anyString());
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.NOTIFICATIONS),
                        eq("Marked as read")));
            }
        }

        @Test
        @DisplayName("given action=markAll when doPost then repo.markAllRead and redirectOk")
        void markAll() throws Exception {
            // given
            NotificationsServlet s = new NotificationsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                s.init(config);
                authMe();
                when(req.getParameter(WebConst.Param.ACTION)).thenReturn("markAll");
                web.when(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.NOTIFICATIONS), anyString()))
                        .then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                verify(repo).markAllRead("u1");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.NOTIFICATIONS),
                        eq("All notifications are marked as read")));
            }
        }

        @Test
        @DisplayName("given action=clearAll when doPost then repo.clearAll and redirectOk")
        void clearAll() throws Exception {
            // given
            NotificationsServlet s = new NotificationsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                s.init(config);
                authMe();
                when(req.getParameter(WebConst.Param.ACTION)).thenReturn("clearAll");
                web.when(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.NOTIFICATIONS), anyString()))
                        .then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                verify(repo).clearAll("u1");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.NOTIFICATIONS),
                        eq("The list has been cleared")));
            }
        }

        @Test
        @DisplayName("given unknown action when doPost then redirectErr")
        void unknownAction() throws Exception {
            // given
            NotificationsServlet s = new NotificationsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(req.getSession(true)).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("???");
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                s.init(config);
                // when
                s.doPost(req, resp);
                // then
                verifyNoInteractions(repo);
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.NOTIFICATIONS),
                        eq("Unknown action")));
            }
        }

        @Test
        @DisplayName("given repo throws when doPost then logs and redirectErr with message")
        void repoThrows() throws Exception {
            // given
            NotificationsServlet s = new NotificationsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.NOTIFY_REPO, NotificationRepository.class))
                        .thenReturn(repo);
                s.init(config);
                authMe();
                when(req.getParameter(WebConst.Param.ACTION)).thenReturn("markAll");
                doThrow(new RuntimeException("boom")).when(repo).markAllRead("u1");
                when(s.getServletContext()).thenReturn(ctx);
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.NOTIFICATIONS), anyString()))
                        .then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                verify(repo).markAllRead("u1");
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.NOTIFICATIONS),
                        eq("Error: boom")));
                verify(ctx).log(startsWith("Notifications POST error"), any(RuntimeException.class));
            }
        }
    }
}