package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.user.UserStats;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.service.user.UserStatsService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("ProfileServlet (unit)")
@ExtendWith(MockitoExtension.class)
class ProfileServletTest {

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
    UserService userService;
    @Mock
    UserStatsService userStats;

    private ProfileServlet sut;

    private static User user(String name) {
        User u = new User();
        u.setUserId("u1");
        u.setUserName(name);
        return u;
    }

    private void initWith(boolean statsPresent) {
        sut = new ProfileServlet();
        when(config.getServletContext()).thenReturn(ctx);
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                    .thenReturn(userService);
            if (statsPresent) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class))
                        .thenReturn(userStats);
            } else {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class))
                        .thenThrow(new IllegalStateException("no stats"));
            }
            assertDoesNotThrow(() -> sut.init(config));
        }
    }

    @Nested
    @DisplayName("init(config)")
    class InitPhase {
        @Test
        @DisplayName("Given userService and userStats present — When init — Then OK")
        void initOkWithStats() {
            // Given
            sut = new ProfileServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class))
                        .thenReturn(userStats);
                // When / Then
                assertDoesNotThrow(() -> sut.init(config));
            }
        }

        @Test
        @DisplayName("Given userStats missing — When init — Then still OK (stats=null)")
        void initOkWithoutStats() {
            // Given
            sut = new ProfileServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_STATS_SERVICE, UserStatsService.class))
                        .thenThrow(new IllegalStateException("no stats"));
                // When / Then
                assertDoesNotThrow(() -> sut.init(config));
            }
        }

        @Test
        @DisplayName("Given userService missing — When init — Then throws IllegalStateException")
        void initFailsWhenUserServiceMissing() {
            // Given
            sut = new ProfileServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenThrow(new IllegalStateException("no userService"));
                // When / Then
                assertThrows(IllegalStateException.class, () -> sut.init(config));
            }
        }
    }

    @Nested
    @DisplayName("doGet(req, resp)")
    class DoGet {

        @BeforeEach
        void setUp() throws ServletException {
            initWith(true);
            when(req.getSession()).thenReturn(session);
        }

        @Test
        @DisplayName("Given unauthenticated — When doGet — Then forward PROFILE without stats")
        void unauthenticatedForwards() throws Exception {
            // Given
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.copyParamsToAttrs(eq(req), anyString(), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.OK)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                // When
                sut.doGet(req, resp);
                // Then
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.PROFILE)));
                verify(session).getAttribute(WebConst.Attr.USER);
                verify(req, never()).setAttribute(eq(WebConst.Attr.USER), any());
                verifyNoInteractions(userStats);
            }
        }

        @Test
        @DisplayName("Given authenticated and stats available — When doGet — Then sets USER and stats, forward PROFILE")
        void authenticatedWithStats() throws Exception {
            // Given
            User me = user("Mike");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            UserStats stats = mock(UserStats.class);
            when(userStats.statsOf("u1")).thenReturn(stats);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.copyParamsToAttrs(eq(req), anyString(), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.OK)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                // When
                sut.doGet(req, resp);
                // Then
                verify(req).setAttribute(WebConst.Attr.USER, me);
                verify(req).setAttribute("stats", stats);
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.PROFILE)));
            }
        }

        @Test
        @DisplayName("Given authenticated and stats service absent — When doGet — Then only USER set, forward PROFILE")
        void authenticatedWithoutStats() throws Exception {
            // Given
            initWith(false);
            when(req.getSession()).thenReturn(session);
            User me = user("Mike");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.copyParamsToAttrs(eq(req), anyString(), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.OK)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                // When
                sut.doGet(req, resp);
                // Then
                verify(req).setAttribute(WebConst.Attr.USER, me);
                verify(req, never()).setAttribute(eq("stats"), any());
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.PROFILE)));
            }
        }
    }

    @Nested
    @DisplayName("doPost(req, resp)")
    class DoPost {

        @BeforeEach
        void setUp() throws ServletException {
            initWith(true);
            when(req.getSession()).thenReturn(session);
        }

        @Test
        @DisplayName("Given unauthenticated — When doPost — Then redirect to /login and return")
        void unauthenticatedRedirectsLogin() throws Exception {
            // Given
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.LOGIN), isNull()));
                verifyNoInteractions(userService);
            }
        }

        @Test
        @DisplayName("Given action=updateName — When valid name — Then userService.updateProfile, set session user, redirectOk")
        void updateNameHappy() throws Exception {
            // Given
            User me = user("Old");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("updateName");
            when(req.getParameter("displayName")).thenReturn("  New Name  ");
            User updated = user("New Name");
            when(userService.updateProfile("u1", "New Name")).thenReturn(updated);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("  New Name  ")).thenReturn("New Name");
                // When
                sut.doPost(req, resp);
                // Then
                verify(userService).updateProfile("u1", "New Name");
                verify(session).setAttribute(WebConst.Attr.USER, updated);
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.PROFILE),
                        eq("Display name changed successfully")));
            }
        }

        @Test
        @DisplayName("Given action=updateName — When empty name — Then redirectErr with message")
        void updateNameValidationError() throws Exception {
            // Given
            User me = user("Old");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("updateName");
            when(req.getParameter("displayName")).thenReturn("  ");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("  ")).thenReturn(null);
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.PROFILE),
                        eq("The name cannot be empty.")));
                verify(userService, never()).updateProfile(anyString(), anyString());
            }
        }

        @Test
        @DisplayName("Given action=changePassword — When valid data — Then changePassword, refresh user, redirectOk")
        void changePasswordHappy() throws Exception {
            // Given
            User me = user("Mike");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("changePassword");
            when(req.getParameter(WebConst.Param.CURRENT_PASSWORD)).thenReturn(" cur ");
            when(req.getParameter(WebConst.Param.NEW_PASSWORD)).thenReturn(" newpass ");
            when(req.getParameter(WebConst.Param.CONFIRM_PASSWORD)).thenReturn(" newpass ");
            User fresh = user("Mike");
            when(userService.findById("u1")).thenReturn(Optional.of(fresh));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull(" cur ")).thenReturn("cur");
                web.when(() -> Web.trimOrNull(" newpass ")).thenReturn("newpass");
                web.when(() -> Web.trimOrNull(" newpass ")).thenReturn("newpass");
                // When
                sut.doPost(req, resp);
                // Then
                verify(userService).changePassword("u1", "cur", "newpass");
                verify(userService).findById("u1");
                verify(session).setAttribute(WebConst.Attr.USER, fresh);
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.PROFILE),
                        eq("Password changed successfully")));
            }
        }

        @Test
        @DisplayName("Given action=changePassword — When wrong current — Then redirectErr 'current password is incorrect'")
        void changePasswordWrongCurrent() throws Exception {
            // Given
            User me = user("Mike");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("changePassword");
            when(req.getParameter(WebConst.Param.CURRENT_PASSWORD)).thenReturn("c");
            when(req.getParameter(WebConst.Param.NEW_PASSWORD)).thenReturn("x12345");
            when(req.getParameter(WebConst.Param.CONFIRM_PASSWORD)).thenReturn("x12345");
            doThrow(new SecurityException("bad")).when(userService).changePassword("u1", "c", "x12345");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("c")).thenReturn("c");
                web.when(() -> Web.trimOrNull("x12345")).thenReturn("x12345");
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.PROFILE),
                        eq("The current password is incorrect")));
            }
        }

        @Test
        @DisplayName("Given action=changePassword — When confirm mismatch — Then redirectErr with message")
        void changePasswordConfirmMismatch() throws Exception {
            // Given
            User me = user("Mike");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("changePassword");
            when(req.getParameter(WebConst.Param.CURRENT_PASSWORD)).thenReturn("cur");
            when(req.getParameter(WebConst.Param.NEW_PASSWORD)).thenReturn("abc123");
            when(req.getParameter(WebConst.Param.CONFIRM_PASSWORD)).thenReturn("zzz");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("cur")).thenReturn("cur");
                web.when(() -> Web.trimOrNull("abc123")).thenReturn("abc123");
                web.when(() -> Web.trimOrNull("zzz")).thenReturn("zzz");
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.PROFILE),
                        eq("The new password and the confirmation don't match")));
                verify(userService, never()).changePassword(anyString(), anyString(), anyString());
            }
        }

        @Test
        @DisplayName("Given unknown action — When doPost — Then redirectErr 'Unknown action'")
        void unknownAction() throws Exception {
            // Given
            User me = user("Mike");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("whatever");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.PROFILE),
                        eq("Unknown action")));
            }
        }

        @Test
        @DisplayName("Given changePassword — When unexpected exception — Then redirectErr INTERNAL_ERROR")
        void unexpectedException() throws Exception {
            // Given
            User me = user("Mike");
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("changePassword");
            when(req.getParameter(WebConst.Param.CURRENT_PASSWORD)).thenReturn("c");
            when(req.getParameter(WebConst.Param.NEW_PASSWORD)).thenReturn("n123456");
            when(req.getParameter(WebConst.Param.CONFIRM_PASSWORD)).thenReturn("n123456");
            doThrow(new RuntimeException("boom")).when(userService).changePassword(anyString(), anyString(), anyString());
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("c")).thenReturn("c");
                web.when(() -> Web.trimOrNull("n123456")).thenReturn("n123456");
                // When
                sut.doPost(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.PROFILE),
                        eq(WebConst.Msg.INTERNAL_ERROR)));
            }
        }
    }
}