package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.user.UserStats;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.service.user.UserStatsService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPublicProfileServlet")
class UserPublicProfileServletTest {

    @Mock
    ServletConfig config;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    UserService userService;
    @Mock
    UserStatsService userStats;
    @Mock
    User user;

    UserPublicProfileServlet subject;

    private void ensureSubject() {
        if (subject == null) subject = new UserPublicProfileServlet();
    }

    private <T> T getField(String name, Class<T> type) {
        try {
            Field f = UserPublicProfileServlet.class.getDeclaredField(name);
            f.setAccessible(true);
            return type.cast(f.get(subject));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Nested
    @DisplayName("init")
    class InitBlock {

        @BeforeEach
        void setUp() {
            ensureSubject();
        }

        @Test
        @DisplayName("given ctx has USER_SERVICE and USER_STATS_SERVICE when init then fields set")
        void should_Init_All_When_BeansPresent() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                // given
                ServletContext sc = mock(ServletContext.class);
                when(config.getServletContext()).thenReturn(sc);
                web.when(() -> Web.ctxBean(eq(sc), eq(WebConst.Ctx.USER_SERVICE), eq(UserService.class)))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(eq(sc), eq(WebConst.Ctx.USER_STATS_SERVICE), eq(UserStatsService.class)))
                        .thenReturn(userStats);
                // when
                subject.init(config);
                // then
                assertSame(userService, getField("userService", UserService.class));
                assertSame(userStats, getField("userStatsService", UserStatsService.class));
            }
        }

        @Test
        @DisplayName("edge: USER_STATS_SERVICE missing -> init keeps userStatsService=null")
        void should_Allow_Null_UserStats_When_Missing() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                ServletContext sc = mock(ServletContext.class);
                when(config.getServletContext()).thenReturn(sc);
                web.when(() -> Web.ctxBean(eq(sc), eq(WebConst.Ctx.USER_SERVICE), eq(UserService.class)))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(eq(sc), eq(WebConst.Ctx.USER_STATS_SERVICE), eq(UserStatsService.class)))
                        .thenThrow(new IllegalStateException("no stats"));
                // when
                subject.init(config);
                // then
                assertSame(userService, getField("userService", UserService.class));
                assertNull(getField("userStatsService", UserStatsService.class));
            }
        }
    }

    @Nested
    @DisplayName("doGet")
    class DoGetBlock {

        @BeforeEach
        void setUp() {
            ensureSubject();
            try {
                Field f1 = UserPublicProfileServlet.class.getDeclaredField("userService");
                f1.setAccessible(true);
                f1.set(subject, userService);
                Field f2 = UserPublicProfileServlet.class.getDeclaredField("userStatsService");
                f2.setAccessible(true);
                f2.set(subject, userStats);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        @Test
        @DisplayName("edge: id==null -> profileUser=null, forward to USER_PUBLIC")
        void should_Forward_With_NullProfile_When_IdNull() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                web.when(() -> Web.copyParamsToAttrs(eq(req), eq(WebConst.Attr.OK), eq(WebConst.Attr.ERROR)))
                        .thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), eq(WebConst.Attr.OK))).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), eq(WebConst.Attr.ERROR))).thenAnswer(inv -> null);
                when(req.getParameter("id")).thenReturn(null);
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USER_PUBLIC)))
                        .thenAnswer(inv -> null);
                // when
                subject.doGet(req, resp);
                // then
                verify(req).setAttribute("profileUser", null);
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.USER_PUBLIC));
                verifyNoInteractions(userService, userStats);
            }
        }

        @Test
        @DisplayName("given id but user not found -> profileUser=null, forward")
        void should_Forward_With_NullProfile_When_UserNotFound() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                web.when(() -> Web.copyParamsToAttrs(eq(req), eq(WebConst.Attr.OK), eq(WebConst.Attr.ERROR)))
                        .thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), eq(WebConst.Attr.OK))).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), eq(WebConst.Attr.ERROR))).thenAnswer(inv -> null);
                when(req.getParameter("id")).thenReturn("u1");
                web.when(() -> Web.trimOrNull("u1")).thenReturn("u1");
                when(userService.findById("u1")).thenReturn(Optional.empty());
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USER_PUBLIC)))
                        .thenAnswer(inv -> null);
                // when
                subject.doGet(req, resp);
                // then
                verify(userService).findById("u1");
                verify(req).setAttribute("profileUser", null);
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.USER_PUBLIC));
                verifyNoInteractions(userStats);
            }
        }

        @Test
        @DisplayName("given user found and stats service present -> sets profileUser and stats, forward")
        void should_Set_Profile_And_Stats_When_Present() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                web.when(() -> Web.copyParamsToAttrs(eq(req), eq(WebConst.Attr.OK), eq(WebConst.Attr.ERROR)))
                        .thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), eq(WebConst.Attr.OK))).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), eq(WebConst.Attr.ERROR))).thenAnswer(inv -> null);
                when(req.getParameter("id")).thenReturn("u2");
                web.when(() -> Web.trimOrNull("u2")).thenReturn("u2");
                when(userService.findById("u2")).thenReturn(Optional.of(user));
                when(user.getUserId()).thenReturn("u2");
                UserStats stats = mock(UserStats.class);
                when(userStats.statsOf("u2")).thenReturn(stats);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USER_PUBLIC)))
                        .thenAnswer(inv -> null);
                // when
                subject.doGet(req, resp);
                // then
                verify(req).setAttribute("profileUser", user);
                verify(req).setAttribute("stats", stats);
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.USER_PUBLIC));
            }
        }

        @Test
        @DisplayName("given user found but stats service is null -> sets profileUser only, forward")
        void should_Set_Profile_Only_When_NoStatsService() throws Exception {
            try {
                Field f = UserPublicProfileServlet.class.getDeclaredField("userStatsService");
                f.setAccessible(true);
                f.set(subject, null);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // given
                web.when(() -> Web.copyParamsToAttrs(eq(req), eq(WebConst.Attr.OK), eq(WebConst.Attr.ERROR)))
                        .thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), eq(WebConst.Attr.OK))).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), eq(WebConst.Attr.ERROR))).thenAnswer(inv -> null);
                when(req.getParameter("id")).thenReturn("u3");
                web.when(() -> Web.trimOrNull("u3")).thenReturn("u3");
                when(userService.findById("u3")).thenReturn(Optional.of(user));
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USER_PUBLIC)))
                        .thenAnswer(inv -> null);
                // when
                subject.doGet(req, resp);
                // then
                verify(req).setAttribute("profileUser", user);
                // без установки "stats"
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.USER_PUBLIC));
                verifyNoInteractions(userStats);
            }
        }
    }
}