package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.user.UserStats;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.service.user.UserStatsService;
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

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileServlet tests")
class ProfileServletTest {

    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;
    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    RequestDispatcher rd;
    @Mock
    UserService userService;
    @Mock
    UserStatsService userStats;
    @Mock
    User me;
    @Mock
    User updated;
    @Mock
    UserStats stats;

    private ProfileServlet servlet() {
        return new ProfileServlet();
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {
        @Test
        @DisplayName("given user & stats available when doGet then set attrs and forward to PROFILE")
        void user_with_stats() throws Exception {
            // given
            ProfileServlet s = servlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(req.getSession(false)).thenReturn(session);
            when(req.getSession()).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.OK)).thenReturn(null);
            when(session.getAttribute(WebConst.Attr.ERROR)).thenReturn(null);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(me.getUserId()).thenReturn("u1");
            when(userStats.statsOf("u1")).thenReturn(stats);
            when(req.getRequestDispatcher(WebConst.Jsp.PROFILE)).thenReturn(rd);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class))
                        .thenReturn(userStats);
                s.init(config);
                // when
                s.doGet(req, resp);
                // then
                verify(req).setAttribute(WebConst.Attr.USER, me);
                verify(req).setAttribute(eq("stats"), eq(stats));
                verify(rd).forward(req, resp);
            }
        }

        @Test
        @DisplayName("given no user when doGet then just forward to PROFILE (no stats call)")
        void no_user() throws Exception {
            // given
            ProfileServlet s = servlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(req.getSession(false)).thenReturn(session);
            when(req.getSession()).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.OK)).thenReturn(null);
            when(session.getAttribute(WebConst.Attr.ERROR)).thenReturn(null);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            when(req.getRequestDispatcher(WebConst.Jsp.PROFILE)).thenReturn(rd);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class))
                        .thenThrow(new IllegalStateException("no stats"));
                s.init(config);
                // when
                s.doGet(req, resp);
                // then
                verify(rd).forward(req, resp);
                verifyNoInteractions(userStats);
            }
        }
    }

    @Nested
    @DisplayName("doPost")
    class DoPost {
        @Test
        @DisplayName("given no user when doPost then redirect to LOGIN")
        void unauth_redirects_login() throws Exception {
            // given
            ProfileServlet s = servlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(req.getSession()).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class))
                        .thenThrow(new IllegalStateException("no stats"));
                s.init(config);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.LOGIN), isNull()));
            }
        }

        @Test
        @DisplayName("given action=updateName and displayName provided when doPost then update profile + redirectOk")
        void updateName_ok() throws Exception {
            // given
            ProfileServlet s = servlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(req.getSession()).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(me.getUserId()).thenReturn("u1");
            when(userService.updateProfile("u1", "New Name")).thenReturn(updated);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class))
                        .thenThrow(new IllegalStateException("no stats"));
                when(req.getParameter(WebConst.Param.ACTION)).thenReturn("updateName");
                when(req.getParameter("displayName")).thenReturn("New Name");
                s.init(config);
                // when
                s.doPost(req, resp);
                // then
                verify(userService).updateProfile("u1", "New Name");
                verify(session).setAttribute(WebConst.Attr.USER, updated);
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.PROFILE),
                        eq("Password changed successfully")));
            }
        }

        @Test
        @DisplayName("given action=changePassword mismatch confirm when doPost then redirectErr with message")
        void changePassword_mismatch() throws Exception {
            // given
            ProfileServlet s = servlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(req.getSession()).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class))
                        .thenThrow(new IllegalStateException("no stats"));
                when(req.getParameter(WebConst.Param.ACTION)).thenReturn("changePassword");
                web.when(() -> Web.trimOrNull("old")).thenReturn("old");
                web.when(() -> Web.trimOrNull("new1")).thenReturn("new1");
                web.when(() -> Web.trimOrNull("new2")).thenReturn("new2");
                when(req.getParameter(WebConst.Param.CURRENT_PASSWORD)).thenReturn("old");
                when(req.getParameter(WebConst.Param.NEW_PASSWORD)).thenReturn("new1");
                when(req.getParameter(WebConst.Param.CONFIRM_PASSWORD)).thenReturn("new2");
                s.init(config);
                // when
                s.doPost(req, resp);
                // then
                verify(userService, never()).changePassword(anyString(), anyString(), anyString());
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.PROFILE),
                        eq("The new password and the confirmation don't match")));
            }
        }

        @Test
        @DisplayName("given action=changePassword ok when doPost then service called, session updated, redirectOk")
        void changePassword_ok() throws Exception {
            // given
            ProfileServlet s = servlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(req.getSession()).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(me.getUserId()).thenReturn("u1");
            when(userService.findById("u1")).thenReturn(Optional.of(updated));
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class))
                        .thenThrow(new IllegalStateException("no stats"));
                when(req.getParameter(WebConst.Param.ACTION)).thenReturn("changePassword");
                when(req.getParameter(WebConst.Param.CURRENT_PASSWORD)).thenReturn("oldpass");
                when(req.getParameter(WebConst.Param.NEW_PASSWORD)).thenReturn("newpass");
                when(req.getParameter(WebConst.Param.CONFIRM_PASSWORD)).thenReturn("newpass");
                web.when(() -> Web.trimOrNull("oldpass")).thenReturn("oldpass");
                web.when(() -> Web.trimOrNull("newpass")).thenReturn("newpass");
                s.init(config);
                // when
                s.doPost(req, resp);
                // then
                verify(userService).changePassword("u1", "oldpass", "newpass");
                verify(userService).findById("u1");
                verify(session).setAttribute(WebConst.Attr.USER, updated);
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.PROFILE),
                        eq("Password changed successfully")));
            }
        }

        @Test
        @DisplayName("given action=changePassword wrong current when doPost then redirectErr specific message")
        void changePassword_wrong_current() throws Exception {
            // given
            ProfileServlet s = servlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(req.getSession()).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(me.getUserId()).thenReturn("u1");
            doThrow(new SecurityException("bad"))
                    .when(userService).changePassword(eq("u1"), eq("old"), eq("newpass"));
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class))
                        .thenThrow(new IllegalStateException("no stats"));
                when(req.getParameter(WebConst.Param.ACTION)).thenReturn("changePassword");
                when(req.getParameter(WebConst.Param.CURRENT_PASSWORD)).thenReturn("old");
                when(req.getParameter(WebConst.Param.NEW_PASSWORD)).thenReturn("newpass");
                when(req.getParameter(WebConst.Param.CONFIRM_PASSWORD)).thenReturn("newpass");
                web.when(() -> Web.trimOrNull("old")).thenReturn("old");
                web.when(() -> Web.trimOrNull("newpass")).thenReturn("newpass");
                s.init(config);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.PROFILE),
                        eq("The current password is incorrect")));
            }
        }

        @Test
        @DisplayName("given unknown action when doPost then redirectErr('Unknown action')")
        void unknown_action() throws Exception {
            // given
            ProfileServlet s = servlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(req.getSession()).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("???");
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class))
                        .thenThrow(new IllegalStateException("no stats"));
                s.init(config);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.PROFILE),
                        eq("Unknown action")));
                verifyNoInteractions(userService);
            }
        }
    }
}