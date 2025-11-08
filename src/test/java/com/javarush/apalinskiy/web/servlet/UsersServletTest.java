package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.impl.user.DefaultUserService;
import com.javarush.apalinskiy.service.user.UserService;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UsersServlet (unit)")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class UsersServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    UserService userService;

    private UsersServlet sut;

    private static User user(String id, String login, String name) {
        User u = new User();
        u.setUserId(id);
        u.setUserLogin(login);
        u.setUserName(name);
        return u;
    }

    @Nested
    @DisplayName("init(config)")
    class InitPhase {

        @Test
        @DisplayName("Given userService bean present — When init — Then resolves via Web.ctxBean")
        void initOk() {
            // Given
            sut = new UsersServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                // When / Then
                assertDoesNotThrow(() -> sut.init(config));
                web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class));
            }
        }

        @Test
        @DisplayName("Given userService missing — When init — Then IllegalStateException")
        void initMissing() {
            // Given
            sut = new UsersServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenThrow(new IllegalStateException("no bean"));
                // When / Then
                assertThrows(IllegalStateException.class, () -> sut.init(config));
            }
        }
    }

    @Nested
    @DisplayName("doGet(req, resp)")
    class DoGet {

        @BeforeEach
        void setUp() throws Exception {
            // Given
            sut = new UsersServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                // When
                sut.init(config);
                // Then
            }
        }

        @Test
        @DisplayName("Given q=null and page=2 — When doGet — Then userService.findPage(2,10) and attrs filled, forward USERS")
        void listPaged() throws Exception {
            // Given
            when(req.getParameter("q")).thenReturn(null);
            when(req.getParameter("page")).thenReturn("2");
            var u1 = user("u1", "mike", "Mike");
            var u2 = user("u2", "ann", "Ann");
            DefaultUserService.PagedResult<User> pg =
                    new DefaultUserService.PagedResult<>(List.of(u1, u2), 25L, 2, 10);
            when(userService.findPage(2, 10)).thenReturn(pg);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.copyParamsToAttrs(eq(req), anyString(), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                // When
                sut.doGet(req, resp);
                // Then
                verify(userService).findPage(2, 10);
                verify(req).setAttribute("users", pg.items());
                verify(req).setAttribute("page", pg.page());
                verify(req).setAttribute("size", pg.size());
                verify(req).setAttribute("total", pg.total());
                verify(req).setAttribute("pages", pg.totalPages());
                verify(req).setAttribute("offset", pg.offset());
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USERS)));
            }
        }

        @Test
        @DisplayName("Given q=null and page not a number — When doGet — Then page=1 used")
        void listPageParseError() throws Exception {
            // Given
            when(req.getParameter("q")).thenReturn(null);
            when(req.getParameter("page")).thenReturn("abc");
            DefaultUserService.PagedResult<User> pg =
                    new DefaultUserService.PagedResult<>(List.of(), 0L, 1, 10);
            when(userService.findPage(1, 10)).thenReturn(pg);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.copyParamsToAttrs(eq(req), anyString(), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                // When
                sut.doGet(req, resp);
                // Then
                verify(userService).findPage(1, 10);
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USERS)));
            }
        }

        @Test
        @DisplayName("Given q='u9' and user found by id — When doGet — Then single result set, size=1, forward")
        void searchByIdHit() throws Exception {
            // Given
            when(req.getParameter("q")).thenReturn("u9");
            var found = user("u9", "john", "John");
            when(userService.findById("u9")).thenReturn(Optional.of(found));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.copyParamsToAttrs(eq(req), anyString(), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.trimOrNull("u9")).thenReturn("u9");
                // When
                sut.doGet(req, resp);
                // Then
                verify(userService).findById("u9");
                verify(req).setAttribute("users", List.of(found));
                verify(req).setAttribute("page", 1);
                verify(req).setAttribute("size", 1);
                verify(req).setAttribute("total", 1);
                verify(req).setAttribute("pages", 1);
                verify(req).setAttribute("offset", 0);
                verify(req).setAttribute("q", "u9");
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USERS)));
            }
        }

        @Test
        @DisplayName("Given q='MiKe' and id miss but login hit — When doGet — Then single login result, forward")
        void searchByLoginHit() throws Exception {
            // Given
            when(req.getParameter("q")).thenReturn("MiKe");
            when(userService.findById("MiKe")).thenReturn(Optional.empty());
            var hit = user("u1", "mike", "Mike");
            when(userService.findByLogin("mike")).thenReturn(Optional.of(hit));
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.copyParamsToAttrs(eq(req), anyString(), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.trimOrNull("MiKe")).thenReturn("MiKe");
                // When
                sut.doGet(req, resp);
                // Then
                verify(userService).findById("MiKe");
                verify(userService).findByLogin("mike");
                verify(req).setAttribute("users", List.of(hit));
                verify(req).setAttribute("size", 1);
                verify(req).setAttribute("total", 1);
                verify(req).setAttribute("pages", 1);
                verify(req).setAttribute("offset", 0);
                verify(req).setAttribute("q", "MiKe");
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USERS)));
            }
        }

        @Test
        @DisplayName("Given q has no matches — When doGet — Then empty list, size=0, forward")
        void searchMiss() throws Exception {
            // Given
            when(req.getParameter("q")).thenReturn("zzz");
            when(userService.findById("zzz")).thenReturn(Optional.empty());
            when(userService.findByLogin("zzz")).thenReturn(Optional.empty());
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.copyParamsToAttrs(eq(req), anyString(), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.trimOrNull("zzz")).thenReturn("zzz");
                // When
                sut.doGet(req, resp);
                // Then
                verify(userService).findById("zzz");
                verify(userService).findByLogin("zzz");
                verify(req).setAttribute("users", List.of());
                verify(req).setAttribute("size", 0);
                verify(req).setAttribute("total", 0);
                verify(req).setAttribute("pages", 1);
                verify(req).setAttribute("offset", 0);
                verify(req).setAttribute("q", "zzz");
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USERS)));
            }
        }
    }
}