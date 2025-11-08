package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.user.UserService;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@DisplayName("MyCustomQuestsServlet (unit)")
@ExtendWith(MockitoExtension.class)
class MyCustomQuestsServletTest {

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
    QuestAuthoringService authoring;
    @Mock
    UserService userService;

    private MyCustomQuestsServlet sut;

    private static User user() {
        User u = new User();
        u.setUserId("u1");
        return u;
    }

    private void initOk() throws ServletException {
        sut = new MyCustomQuestsServlet();
        when(config.getServletContext()).thenReturn(ctx);
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                    .thenReturn(authoring);
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                    .thenReturn(userService);
            sut.init(config);
            web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class));
            web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class));
        }
    }

    @Nested
    @DisplayName("init(config)")
    class InitPhase {

        @Test
        @DisplayName("Given both beans in context — When init — Then servlet initializes OK")
        void initSuccess() {
            sut = new MyCustomQuestsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenReturn(authoring);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                assertDoesNotThrow(() -> sut.init(config));
            }
        }

        @Test
        @DisplayName("Given beans missing — When init — Then throws UnavailableException")
        void initFailsWhenBeansMissing() {
            sut = new MyCustomQuestsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenThrow(new IllegalStateException("no bean"));
                assertThrows(ServletException.class, () -> sut.init(config));
            }
        }
    }

    @Nested
    @DisplayName("doGet(req, resp)")
    class DoGet {

        @BeforeEach
        void setUp() throws ServletException {
            initOk();
            when(req.getSession()).thenReturn(session);
        }

        @Test
        @DisplayName("Given unauthenticated — When doGet — Then redirect to /login and return")
        void unauthRedirectsToLogin() throws Exception {
            // Given
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // When
                sut.doGet(req, resp);
                // Then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.LOGIN), eq(Map.of())));
                web.verify(() -> Web.forward(any(), any(), anyString()), times(0));
                verifyNoInteractions(authoring);
            }
        }

        @Test
        @DisplayName("Given authenticated and params — When doGet — Then list owner quests, applyPagedList and forward")
        void happyPath() throws Exception {
            // Given
            User me = user();
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            String q = "demo";
            int page = 2;
            int size = 5;
            QuestAuthoringService.Paged<CustomQuest> paged =
                    new QuestAuthoringService.Paged<>(List.of(mock(CustomQuest.class)), /*total*/7, page, size);
            when(authoring.listOwnerFromCatalogPaged(eq("u1"), eq(q), eq(page), eq(size), eq(true)))
                    .thenReturn(paged);
            when(req.getContextPath()).thenReturn("/app");
            when(req.getServletPath()).thenReturn("/my_quests");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(req, WebConst.Attr.FLASH)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                web.when(() -> Web.extract(req)).thenReturn(new Web.Params(q, page, size));
                // When
                sut.doGet(req, resp);
                // Then
                verify(authoring).listOwnerFromCatalogPaged("u1", q, page, size, true);
                web.verify(() -> Web.applyPagedList(eq(req), same(paged), same(userService), eq(q)));
                verify(req).setAttribute("pageTitleKey", "my.quests");
                verify(req).setAttribute("showOwnerActions", Boolean.TRUE);
                verify(req).setAttribute("selfUrl", "/app/my_quests");
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.QUESTS_LIST)));
            }
        }

        @Test
        @DisplayName("Given authenticated and null q — When doGet — Then passes null to authoring and applies page/size")
        void nullQueryString() throws Exception {
            // Given
            User me = user();
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(me);
            int page = 1;
            int size = 10;
            QuestAuthoringService.Paged<CustomQuest> paged =
                    new QuestAuthoringService.Paged<>(List.of(), 0, page, size);
            when(authoring.listOwnerFromCatalogPaged(eq("u1"), isNull(), eq(page), eq(size), eq(true)))
                    .thenReturn(paged);
            when(req.getContextPath()).thenReturn("");
            when(req.getServletPath()).thenReturn(WebConst.Path.MY_QUESTS);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.pullFlash(req, WebConst.Attr.FLASH)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                web.when(() -> Web.extract(req)).thenReturn(new Web.Params(null, page, size));
                // When
                sut.doGet(req, resp);
                // Then
                verify(authoring).listOwnerFromCatalogPaged("u1", null, page, size, true);
                web.verify(() -> Web.applyPagedList(eq(req), same(paged), same(userService), isNull()));
                verify(req).setAttribute("selfUrl", WebConst.Path.MY_QUESTS);
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.QUESTS_LIST)));
            }
        }
    }
}