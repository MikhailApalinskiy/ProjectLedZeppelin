package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.*;


@DisplayName("AllCustomQuestsServlet")
@ExtendWith(MockitoExtension.class)
class AllCustomQuestsServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    QuestAuthoringService authoring;
    @Mock
    UserService userService;
    @Mock
    CustomQuest q1, q2;

    private static void setField(Object target, String fieldName, Object value) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass(); // пробуем в родителе
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot set field '" + fieldName + "'", e);
            }
        }
        throw new RuntimeException(new NoSuchFieldException(fieldName));
    }

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("given ctx has QuestAuthoringService when init then ok")
        void init_ok() throws Exception {
            // given
            AllCustomQuestsServlet s = new AllCustomQuestsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenReturn(authoring);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                // when
                s.init(config);
                // then
                web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class));
                web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class));
            }
        }

        @Test
        @DisplayName("given no service in ctx when init then UnavailableException")
        void init_missing_service() {
            // given
            AllCustomQuestsServlet s = new AllCustomQuestsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenThrow(new IllegalStateException("no bean"));
                // when / then
                org.junit.jupiter.api.Assertions.assertThrows(UnavailableException.class, () -> s.init(config));
            }
        }
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @Test
        @DisplayName("given catalog items when doGet then filter&attach, attrs set, forward to list jsp")
        void ok_flow() throws Exception {
            // given
            AllCustomQuestsServlet s = new AllCustomQuestsServlet();
            setField(s, "authoring", authoring);
            setField(s, "userService", userService);
            List<CustomQuest> items = List.of(q1, q2);
            when(authoring.listAllFromCatalog()).thenReturn(items);
            when(req.getContextPath()).thenReturn("/app");
            when(req.getServletPath()).thenReturn("/quests");
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.attachOwnerNamesById(eq(req), eq(items), any(UserService.class)))
                        .then(inv -> null);
                web.when(() -> Web.filterAndAttachQuests(eq(req), eq(items))).then(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.QUESTS_LIST))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(authoring).listAllFromCatalog();
                web.verify(() -> Web.attachOwnerNamesById(eq(req), eq(items), any()));
                web.verify(() -> Web.filterAndAttachQuests(eq(req), eq(items)));
                verify(req).setAttribute("pageTitleKey", "all.quests");
                verify(req).setAttribute("showOwnerActions", Boolean.FALSE);
                verify(req).setAttribute("selfUrl", "/app/quests");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.QUESTS_LIST));
            }
        }

        @Test
        @DisplayName("given empty catalog when doGet then still attaches empty, sets attrs, forwards")
        void empty_catalog() throws Exception {
            // given
            AllCustomQuestsServlet s = new AllCustomQuestsServlet();
            setField(s, "authoring", authoring);
            List<CustomQuest> items = List.of();
            when(authoring.listAllFromCatalog()).thenReturn(items);
            when(req.getContextPath()).thenReturn("/ctx");
            when(req.getServletPath()).thenReturn("/all");
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.filterAndAttachQuests(eq(req), eq(items))).then(inv -> null);
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.QUESTS_LIST))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(authoring).listAllFromCatalog();
                web.verify(() -> Web.filterAndAttachQuests(eq(req), eq(items)));
                verify(req).setAttribute("pageTitleKey", "all.quests");
                verify(req).setAttribute("showOwnerActions", Boolean.FALSE);
                verify(req).setAttribute("selfUrl", "/ctx/all");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.QUESTS_LIST));
            }
        }
    }
}