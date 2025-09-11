package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
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

import java.util.List;
import java.util.Map;

import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("MyCustomQuestsServlet")
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
    User user;
    @Mock
    CustomQuest q1;
    @Mock
    CustomQuest q2;

    MyCustomQuestsServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new MyCustomQuestsServlet();
    }

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("given ctx has AuthoringService when init then ok")
        void init_ok() throws Exception {
            // given
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenReturn(authoring);
                // when
                servlet.init(config);
                // then
                web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class));
            }
        }

        @Test
        @DisplayName("given ctx misses AuthoringService when init then UnavailableException")
        void init_missing_throws() {
            // given
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenThrow(new IllegalStateException("no bean"));
                // when / then
                org.junit.jupiter.api.Assertions.assertThrows(
                        UnavailableException.class,
                        () -> servlet.init(config)
                );
            }
        }
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @Test
        @DisplayName("given no user in session when doGet then redirect to /login")
        void unauth_redirects_login() throws Exception {
            // given
            when(req.getSession()).thenReturn(session);
            when(session.getAttribute(WebConst.Attr.USER)).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                when(config.getServletContext()).thenReturn(ctx);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenReturn(authoring);
                servlet.init(config);
                web.when(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.LOGIN), eq(Map.of())))
                        .then(inv -> null);
                // when
                servlet.doGet(req, resp);
                // then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.LOGIN), eq(Map.of())));
                verifyNoMoreInteractions(authoring);
            }
        }

        @Test
        @DisplayName("given user when doGet then filter+attach, page attrs, and forward to quests_list")
        void auth_ok_flow() throws Exception {
            // given
            when(req.getSession()).thenReturn(session);
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute(anyString())).thenAnswer(inv -> {
                String key = inv.getArgument(0, String.class);
                if (WebConst.Attr.USER.equals(key)) return user;
                return null;
            });
            when(user.getUserLogin()).thenReturn("me");
            List<CustomQuest> items = List.of(q1, q2);
            when(authoring.listOwnerFromCatalog("me")).thenReturn(items);
            when(req.getContextPath()).thenReturn("/app");
            when(req.getServletPath()).thenReturn("/my/quests");
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                when(config.getServletContext()).thenReturn(ctx);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenReturn(authoring);
                servlet.init(config);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.FLASH)).then(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).then(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.QUESTS_LIST))).then(inv -> null);
                // when
                servlet.doGet(req, resp);
                // then
                verify(authoring).listOwnerFromCatalog("me");
                web.verify(() -> Web.pullFlash(req, WebConst.Attr.FLASH));
                web.verify(() -> Web.pullFlash(req, WebConst.Attr.ERROR));
                web.verify(() -> Web.filterAndAttachQuests(req, items));
                verify(req).setAttribute("pageTitleKey", "my.quests");
                verify(req).setAttribute("showOwnerActions", Boolean.TRUE);
                verify(req).setAttribute("selfUrl", "/app/my/quests");
                web.verify(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.QUESTS_LIST)));
            }
        }
    }
}