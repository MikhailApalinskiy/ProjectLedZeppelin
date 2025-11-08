package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UserPublishedQuestsServlet (unit)")
@ExtendWith(MockitoExtension.class)
class UserPublishedQuestsServletTest {

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
    QuestAuthoringService authoring;

    private UserPublishedQuestsServlet sut;

    private static User user(String id, String name, Role role) {
        User u = new User();
        u.setUserId(id);
        u.setUserName(name);
        u.setRole(role);
        return u;
    }

    private static CustomQuest cq(String id, String ownerId, String name) {
        CustomQuest q = new CustomQuest();
        q.setId(id);
        q.setOwnerId(ownerId);
        q.setName(name);
        return q;
    }

    private static QuestAuthoringService.Paged<CustomQuest> paged(List<CustomQuest> items, int total, int size) {
        return new QuestAuthoringService.Paged<>(items, total, 1, size);
    }

    @Nested
    @DisplayName("init(config)")
    class InitPhase {

        @Test
        @DisplayName("Given both beans present — When init — Then OK")
        void initOk() {
            // Given
            sut = new UserPublishedQuestsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenReturn(authoring);
                // When / Then
                assertDoesNotThrow(() -> sut.init(config));
            }
        }

        @Test
        @DisplayName("Given beans missing — When init — Then UnavailableException")
        void initFails() {
            // Given
            sut = new UserPublishedQuestsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenThrow(new IllegalStateException("no bean"));
                // When
                UnavailableException ex = assertThrows(UnavailableException.class, () -> sut.init(config));
                // Then
                assertTrue(ex.getMessage().contains("Required services not found"));
            }
        }
    }

    @Nested
    @DisplayName("doGet(req, resp)")
    class DoGet {

        @BeforeEach
        void setup() throws ServletException {
            sut = new UserPublishedQuestsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenReturn(authoring);
                sut.init(config);
            }
        }

        @Test
        @DisplayName("Given missing id — When doGet — Then redirectErr HOME 'User id is required'")
        void missingId() throws Exception {
            // Given
            when(req.getParameter("id")).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.trimOrNull(null)).thenReturn(null);
                // When
                sut.doGet(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp),
                        eq(WebConst.Path.HOME), eq("User id is required")));
                verifyNoInteractions(userService, authoring);
            }
        }

        @Test
        @DisplayName("Given user not found — When doGet — Then redirectErr HOME 'User not found'")
        void userNotFound() throws Exception {
            // Given
            when(req.getParameter("id")).thenReturn("  u3  ");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.trimOrNull("  u3  ")).thenReturn("u3");
                when(userService.findById("u3")).thenReturn(Optional.empty());
                // When
                sut.doGet(req, resp);
                // Then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp),
                        eq(WebConst.Path.HOME), eq("User not found")));
                verify(userService).findById("u3");
                verifyNoInteractions(authoring);
            }
        }

        @Test
        @DisplayName("Given viewer is anon — When doGet — Then listOwnerFromCatalogPaged(..., onlyLive=true) and forward USER_QUESTS")
        void anonViewer() throws Exception {
            // Given
            when(req.getParameter("id")).thenReturn("u42");
            when(req.getContextPath()).thenReturn("/app");
            when(req.getServletPath()).thenReturn("/users/quests");
            User viewed = user("u42", "Alice", Role.USER);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.trimOrNull("u42")).thenReturn("u42");
                when(req.getAttribute(WebConst.Attr.USER)).thenReturn(null);
                when(req.getSession()).thenReturn(session);
                when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
                when(userService.findById("u42")).thenReturn(Optional.of(viewed));
                var params = new Web.Params("Mike", 2, 5);
                web.when(() -> Web.extract(req)).thenReturn(params);
                var items = List.of(cq("q1", "u42", "First"));
                when(authoring.listOwnerFromCatalogPaged("u42", "Mike", 2, 5, false))
                        .thenReturn(paged(items, 1, 5));
                // When
                sut.doGet(req, resp);
                // Then
                verify(req).setAttribute("ownerNameById", Map.of("u42", "Alice"));
                verify(req).setAttribute("viewUser", viewed);
                verify(req).setAttribute("items", items);
                verify(req).setAttribute("total", 1);
                verify(req).setAttribute("pages", 1);
                verify(req).setAttribute("page", 1);
                verify(req).setAttribute("q", "Mike");
                verify(req).setAttribute("selfPathOnly", "/app/users/quests");
                verify(req).setAttribute("viewUserId", "u42");
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USER_QUESTS)));
            }
        }

        @Test
        @DisplayName("Given viewer is the owner — When doGet — Then onlyLive=false")
        void viewerIsOwner() throws Exception {
            // Given
            when(req.getParameter("id")).thenReturn("u1");
            when(req.getContextPath()).thenReturn("");
            when(req.getServletPath()).thenReturn("/users/quests");
            User viewed = user("u1", "Mike", Role.USER);
            User me = user("u1", "Mike", Role.USER);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.trimOrNull("u1")).thenReturn("u1");
                when(userService.findById("u1")).thenReturn(Optional.of(viewed));
                when(req.getAttribute(WebConst.Attr.USER)).thenReturn(null);
                when(req.getSession()).thenReturn(session);
                when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
                var params = new Web.Params(null, 1, 10);
                web.when(() -> Web.extract(req)).thenReturn(params);
                when(authoring.listOwnerFromCatalogPaged("u1", null, 1, 10, true))
                        .thenReturn(paged(Collections.emptyList(), 0, 10));
                // When
                sut.doGet(req, resp);
                // Then
                verify(authoring).listOwnerFromCatalogPaged("u1", null, 1, 10, true);
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USER_QUESTS)));
            }
        }

        @Test
        @DisplayName("Given viewer is admin — When doGet — Then onlyLive=false")
        void viewerIsAdmin() throws Exception {
            // Given
            when(req.getParameter("id")).thenReturn("u2");
            when(req.getContextPath()).thenReturn("/ctx");
            when(req.getServletPath()).thenReturn("/users/quests");
            User viewed = user("u2", "Ann", Role.USER);
            User admin = user("adm", "Root", Role.ADMIN);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(eq(req), anyString())).thenAnswer(inv -> null);
                web.when(() -> Web.trimOrNull("u2")).thenReturn("u2");
                when(userService.findById("u2")).thenReturn(Optional.of(viewed));
                when(req.getAttribute(WebConst.Attr.USER)).thenReturn(admin);
                var params = new Web.Params("x", 3, 7);
                web.when(() -> Web.extract(req)).thenReturn(params);
                when(authoring.listOwnerFromCatalogPaged("u2", "x", 3, 7, true))
                        .thenReturn(paged(List.of(cq("q2", "u2", "Q")), 1, 7));
                // When
                sut.doGet(req, resp);
                // Then
                verify(authoring).listOwnerFromCatalogPaged("u2", "x", 3, 7, true);
                verify(req).setAttribute("selfPathOnly", "/ctx/users/quests");
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.USER_QUESTS)));
            }
        }
    }
}